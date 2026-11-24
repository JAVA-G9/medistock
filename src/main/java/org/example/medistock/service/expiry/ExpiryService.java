package org.example.medistock.service.expiry;

import org.example.medistock.model.ExpiryRecord;
import org.example.medistock.model.MedicineBatch;
import org.example.medistock.model.Notification;
import org.example.medistock.service.notifications.NotificationService;
import org.example.medistock.util.DBConnection;

import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service for medicine batch expiration tracking, automated risk classification,
 * threshold notifications, and batch status lifecycle.
 * Owned by Member 4 (Expiry, Notifications & Reports).
 * Data is fetched and managed in real time directly from the TiDB Cloud MySQL database.
 */
public class ExpiryService {

    private static ExpiryService instance;
    private final NotificationService notificationService;
    private final List<MedicineBatch> dbCache = new CopyOnWriteArrayList<>();
    private final Set<String> notifiedBatchKeys = ConcurrentHashMap.newKeySet();
    private volatile long lastFetchTime = 0;
    private static final long CACHE_TTL_MS = 10_000; // 10-second smart cache to eliminate redundant remote DB round-trips
    private boolean tablesInitialized = false;

    public ExpiryService() {
        this(NotificationService.getInstance());
    }

    public ExpiryService(NotificationService notificationService) {
        this.notificationService = notificationService;
        initDatabase();
    }

    public static synchronized ExpiryService getInstance() {
        if (instance == null) {
            instance = new ExpiryService();
        }
        return instance;
    }

    private synchronized void initDatabase() {
        if (tablesInitialized) return;
        try (Connection con = DBConnection.connectToMySql()) {
            if (con != null) {
                try (Statement st = con.createStatement()) {
                    String ddl = """
                       CREATE TABLE IF NOT EXISTS medicine_batch (
                           batch_id INT AUTO_INCREMENT PRIMARY KEY,
                           medicine_id INT NOT NULL,
                           medicine_name VARCHAR(255) NOT NULL,
                           batch_number VARCHAR(100) NOT NULL,
                           manufacturing_date DATE NOT NULL,
                           expiry_date DATE NOT NULL,
                       
                           quantity_received INT NOT NULL DEFAULT 0,
                           quantity_available INT NOT NULL DEFAULT 0,
                       
                           purchase_price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
                           selling_price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
                       
                           pharmacy_id VARCHAR(50) NOT NULL DEFAULT 'PHARM-001',
                           status VARCHAR(50) NOT NULL DEFAULT 'AVAILABLE',
                           received_date DATE NULL,
                       
                           INDEX idx_expiry (expiry_date),
                           INDEX idx_pharmacy (pharmacy_id),
                           INDEX idx_medicine (medicine_id)
                       );
                    """;
                    st.executeUpdate(ddl);
                    System.out.println("[ExpiryService] Table 'medicine_batch' verified in TiDB Cloud.");
                    tablesInitialized = true;
                }
            }
        } catch (Exception e) {
            System.err.println("[ExpiryService] DB table initialization error: " + e.getMessage());
        }
    }

    /**
     * Saves or updates a medicine batch directly in TiDB Cloud MySQL.
     */
    public synchronized MedicineBatch saveBatch(MedicineBatch batch) {
        if (batch == null) return null;

        try (Connection con = DBConnection.connectToMySql()) {
            if (con != null) {
                // Insert using TiDB Cloud schema (quantity_available, selling_price)
                String sql = """
                    INSERT INTO medicine_batch (medicine_id, medicine_name, batch_number, manufacturing_date, expiry_date, quantity_available, selling_price, pharmacy_id, status)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                        quantity_available = VALUES(quantity_available),
                        selling_price = VALUES(selling_price),
                        expiry_date = VALUES(expiry_date),
                        status = VALUES(status)
                """;
                try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, batch.getMedicineId());
                    ps.setString(2, batch.getMedicineName());
                    ps.setString(3, batch.getBatchNumber());
                    ps.setDate(4, java.sql.Date.valueOf(batch.getManufacturingDate()));
                    ps.setDate(5, java.sql.Date.valueOf(batch.getExpiryDate()));
                    ps.setInt(6, batch.getQuantity());
                    ps.setDouble(7, batch.getUnitPrice());
                    ps.setString(8, batch.getPharmacyId());
                    ps.setString(9, "AVAILABLE");
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            batch.setBatchId(rs.getInt(1));
                        }
                    }
                } catch (SQLException ex) {
                    // Fallback for tables with quantity/unit_price columns
                    String fallbackSql = """
                        INSERT INTO medicine_batch (medicine_id, medicine_name, batch_number, manufacturing_date, expiry_date, quantity, unit_price, pharmacy_id, status)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                            quantity = VALUES(quantity),
                            unit_price = VALUES(unit_price),
                            expiry_date = VALUES(expiry_date),
                            status = VALUES(status)
                    """;
                    try (PreparedStatement ps2 = con.prepareStatement(fallbackSql, Statement.RETURN_GENERATED_KEYS)) {
                        ps2.setInt(1, batch.getMedicineId());
                        ps2.setString(2, batch.getMedicineName());
                        ps2.setString(3, batch.getBatchNumber());
                        ps2.setDate(4, java.sql.Date.valueOf(batch.getManufacturingDate()));
                        ps2.setDate(5, java.sql.Date.valueOf(batch.getExpiryDate()));
                        ps2.setInt(6, batch.getQuantity());
                        ps2.setDouble(7, batch.getUnitPrice());
                        ps2.setString(8, batch.getPharmacyId());
                        ps2.setString(9, "AVAILABLE");
                        ps2.executeUpdate();
                        try (ResultSet rs = ps2.getGeneratedKeys()) {
                            if (rs.next()) {
                                batch.setBatchId(rs.getInt(1));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[ExpiryService] Error saving batch to TiDB Cloud: " + e.getMessage());
        }

        // Keep local cache in sync
        dbCache.removeIf(b -> b.getBatchNumber().equalsIgnoreCase(batch.getBatchNumber()));
        dbCache.add(batch);
        return batch;
    }

    public void invalidateCache() {
        lastFetchTime = 0;
    }

    /**
     * Fetches all medicine batches directly from TiDB Cloud MySQL in real time,
     * joining with the master 'medicine' table to resolve canonical medicine names.
     * Uses a short 10s memory cache to prevent redundant remote round-trips.
     */
    public List<MedicineBatch> fetchBatchesFromDB(String pharmacyId) {
        return fetchBatchesFromDB(pharmacyId, false);
    }

    public List<MedicineBatch> fetchBatchesFromDB(String pharmacyId, boolean forceRefresh) {
        long now = System.currentTimeMillis();
        if (!forceRefresh && !dbCache.isEmpty() && (now - lastFetchTime < CACHE_TTL_MS)) {
            return new ArrayList<>(dbCache);
        }

        List<MedicineBatch> list = new ArrayList<>();
        try (Connection con = DBConnection.connectToMySql()) {
            if (con != null) {
                String sql = """
                    SELECT mb.*, m.medicine_name AS parent_med_name, m.generic_name, m.category, m.strength, m.dosage_form
                    FROM medicine_batch mb
                    LEFT JOIN medicine m ON mb.medicine_id = m.medicine_id
                    WHERE mb.pharmacy_id = ? OR mb.pharmacy_id = 'ALL'
                    ORDER BY mb.expiry_date ASC
                """;
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, pharmacyId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            int qty = 0;
                            try {
                                qty = rs.getInt("quantity_available");
                            } catch (SQLException e1) {
                                try {
                                    qty = rs.getInt("quantity");
                                } catch (SQLException e2) {
                                    try { qty = rs.getInt("quantity_received"); } catch (SQLException ignored) {}
                                }
                            }

                            double price = 0.0;
                            try {
                                price = rs.getDouble("selling_price");
                            } catch (SQLException e1) {
                                try {
                                    price = rs.getDouble("unit_price");
                                } catch (SQLException e2) {
                                    try { price = rs.getDouble("purchase_price"); } catch (SQLException ignored) {}
                                }
                            }

                            LocalDate mfgDate = null;
                            try {
                                java.sql.Date d = rs.getDate("manufacturing_date");
                                if (d != null) mfgDate = d.toLocalDate();
                            } catch (Exception ignored) {}
                            if (mfgDate == null) mfgDate = LocalDate.now().minusMonths(6);

                            LocalDate expDate = null;
                            try {
                                java.sql.Date d = rs.getDate("expiry_date");
                                if (d != null) expDate = d.toLocalDate();
                            } catch (Exception ignored) {}
                            if (expDate == null) expDate = LocalDate.now().plusMonths(6);

                            String medName = "";
                            try {
                                medName = rs.getString("parent_med_name");
                            } catch (Exception ignored) {}
                            if (medName == null || medName.isBlank()) {
                                try {
                                    medName = rs.getString("medicine_name");
                                } catch (Exception ignored) {}
                            }
                            if (medName == null || medName.isBlank()) {
                                medName = "Medicine-" + rs.getInt("medicine_id");
                            }

                            MedicineBatch b = new MedicineBatch(
                                    rs.getInt("batch_id"),
                                    rs.getInt("medicine_id"),
                                    medName,
                                    rs.getString("batch_number"),
                                    mfgDate,
                                    expDate,
                                    qty,
                                    price,
                                    rs.getString("pharmacy_id")
                            );
                            list.add(b);
                        }
                    }
                }
                // If this specific pharmacy has no batches in DB, show the network batches so data is visible
                if (list.isEmpty() && !"PHARM-001".equals(pharmacyId)) {
                    return fetchBatchesFromDB("PHARM-001", forceRefresh);
                }
                // Update local cache with live DB records and mark timestamp
                dbCache.clear();
                dbCache.addAll(list);
                lastFetchTime = System.currentTimeMillis();
                return list;
            }
        } catch (Exception e) {
            System.err.println("[ExpiryService] TiDB fetch failed, using cached data: " + e.getMessage());
        }

        // Return cache only if DB connection failed
        return new ArrayList<>(dbCache);
    }

    /**
     * Checks all medicine batches for expiration status, calculates remaining days dynamically,
     * updates records, and triggers notifications for critical/expired items.
     * Called periodically by ExpiryMonitorthread.
     */
    public synchronized List<ExpiryRecord> checkAndProcessExpiries(String pharmacyId) {
        List<MedicineBatch> batches = fetchBatchesFromDB(pharmacyId);
        List<ExpiryRecord> records = new ArrayList<>();
        List<ExpiryRecord> urgentRecords = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (MedicineBatch b : batches) {
            long days = ChronoUnit.DAYS.between(today, b.getExpiryDate());
            ExpiryRecord.ExpiryStatus status;

            if (days < 0) {
                status = ExpiryRecord.ExpiryStatus.EXPIRED;
            } else if (days <= 15) {
                status = ExpiryRecord.ExpiryStatus.CRITICAL;
            } else if (days <= 30) {
                status = ExpiryRecord.ExpiryStatus.EXPIRING_SOON;
            } else if (days <= 60) {
                status = ExpiryRecord.ExpiryStatus.ATTENTION;
            } else {
                status = ExpiryRecord.ExpiryStatus.SAFE;
            }

            ExpiryRecord record = new ExpiryRecord(
                    "EXP-" + b.getBatchNumber(),
                    b.getBatchId(),
                    b.getMedicineName(),
                    b.getBatchNumber(),
                    b.getExpiryDate(),
                    days,
                    b.getQuantity(),
                    b.getUnitPrice(),
                    b.getPharmacyId(),
                    status
            );
            records.add(record);

            if (record.isUrgent()) {
                urgentRecords.add(record);
                triggerNotificationIfNew(record);
            }
        }

        return urgentRecords;
    }

    private void triggerNotificationIfNew(ExpiryRecord record) {
        String key = record.getBatchNumber() + ":" + record.getStatus();
        if (notifiedBatchKeys.add(key)) {
            String title;
            String message;
            Notification.Priority priority;
            Notification.NotificationType type;

            if (record.getStatus() == ExpiryRecord.ExpiryStatus.EXPIRED) {
                title = "EXPIRED BATCH: " + record.getMedicineName();
                message = String.format("Batch %s expired %d days ago (%d units). Immediately quarantine or dispose.",
                        record.getBatchNumber(), Math.abs(record.getDaysRemaining()), record.getQuantity());
                priority = Notification.Priority.HIGH;
                type = Notification.NotificationType.BATCH_EXPIRED;
            } else if (record.getStatus() == ExpiryRecord.ExpiryStatus.CRITICAL) {
                title = "CRITICAL EXPIRY: " + record.getMedicineName();
                message = String.format("Batch %s expires in only %d days (%d units). Prioritize exchange or dispatch.",
                        record.getBatchNumber(), record.getDaysRemaining(), record.getQuantity());
                priority = Notification.Priority.HIGH;
                type = Notification.NotificationType.EXPIRY_WARNING;
            } else {
                title = "Near-Expiry Alert: " + record.getMedicineName();
                message = String.format("Batch %s expires on %s (%d days remaining).",
                        record.getBatchNumber(), record.getFormattedExpiryDate(), record.getDaysRemaining());
                priority = Notification.Priority.NORMAL;
                type = Notification.NotificationType.EXPIRY_WARNING;
            }

            if (notificationService != null) {
                notificationService.createNotification(record.getPharmacyId(), title, message, type, priority);
            }
        }
    }

    public List<ExpiryRecord> getAllExpiryRecords(String pharmacyId) {
        List<MedicineBatch> batches = fetchBatchesFromDB(pharmacyId);
        List<ExpiryRecord> records = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (MedicineBatch b : batches) {
            long days = ChronoUnit.DAYS.between(today, b.getExpiryDate());
            ExpiryRecord.ExpiryStatus status;

            if (days < 0) {
                status = ExpiryRecord.ExpiryStatus.EXPIRED;
            } else if (days <= 15) {
                status = ExpiryRecord.ExpiryStatus.CRITICAL;
            } else if (days <= 30) {
                status = ExpiryRecord.ExpiryStatus.EXPIRING_SOON;
            } else if (days <= 60) {
                status = ExpiryRecord.ExpiryStatus.ATTENTION;
            } else {
                status = ExpiryRecord.ExpiryStatus.SAFE;
            }

            records.add(new ExpiryRecord(
                    "EXP-" + b.getBatchNumber(),
                    b.getBatchId(),
                    b.getMedicineName(),
                    b.getBatchNumber(),
                    b.getExpiryDate(),
                    days,
                    b.getQuantity(),
                    b.getUnitPrice(),
                    b.getPharmacyId(),
                    status
            ));
        }

        // Sort by days remaining ascending (expired and most urgent first)
        records.sort(Comparator.comparingLong(ExpiryRecord::getDaysRemaining));
        return records;
    }

    public List<ExpiryRecord> filterRecords(String pharmacyId, String query, String statusFilter) {
        List<ExpiryRecord> all = getAllExpiryRecords(pharmacyId);
        String q = query != null ? query.trim().toLowerCase() : "";

        return all.stream().filter(r -> {
            boolean matchesQuery = q.isEmpty()
                    || r.getMedicineName().toLowerCase().contains(q)
                    || r.getBatchNumber().toLowerCase().contains(q);

            boolean matchesStatus = statusFilter == null
                    || "ALL".equalsIgnoreCase(statusFilter)
                    || r.getStatus().name().equalsIgnoreCase(statusFilter);

            return matchesQuery && matchesStatus;
        }).toList();
    }

    /**
     * Quarantines and deletes an expired medicine batch directly from TiDB Cloud MySQL.
     */
    public void disposeBatch(String batchNumber) {
        dbCache.removeIf(b -> b.getBatchNumber().equalsIgnoreCase(batchNumber));
        try (Connection con = DBConnection.connectToMySql()) {
            if (con != null) {
                String sql = "DELETE FROM medicine_batch WHERE batch_number = ?";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, batchNumber);
                    ps.executeUpdate();
                    System.out.println("[ExpiryService] Batch '" + batchNumber + "' deleted from TiDB Cloud.");
                }
            }
        } catch (Exception e) {
            System.err.println("[ExpiryService] Error disposing batch in TiDB Cloud: " + e.getMessage());
        }
    }

    /**
     * Updates a batch expiry date directly in TiDB Cloud MySQL.
     */
    public void updateBatchExpiry(String batchNumber, LocalDate newDate) {
        for (MedicineBatch b : dbCache) {
            if (b.getBatchNumber().equalsIgnoreCase(batchNumber)) {
                b.setExpiryDate(newDate);
            }
        }
        try (Connection con = DBConnection.connectToMySql()) {
            if (con != null) {
                String sql = "UPDATE medicine_batch SET expiry_date = ? WHERE batch_number = ?";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setDate(1, java.sql.Date.valueOf(newDate));
                    ps.setString(2, batchNumber);
                    ps.executeUpdate();
                    System.out.println("[ExpiryService] Batch '" + batchNumber + "' expiry updated in TiDB Cloud.");
                }
            }
        } catch (Exception e) {
            System.err.println("[ExpiryService] Error updating batch expiry in TiDB Cloud: " + e.getMessage());
        }
    }
}
