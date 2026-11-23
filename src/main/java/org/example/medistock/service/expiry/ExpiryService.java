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
 */
public class ExpiryService {

    private static ExpiryService instance;
    private final NotificationService notificationService;
    private final List<MedicineBatch> inMemoryBatches = new CopyOnWriteArrayList<>();
    private final Set<String> notifiedBatchKeys = ConcurrentHashMap.newKeySet();
    private boolean tablesInitialized = false;

    public ExpiryService() {
        this(NotificationService.getInstance());
    }

    public ExpiryService(NotificationService notificationService) {
        this.notificationService = notificationService;
        initDatabase();
        ensureSeedData();
        ensureSeedData();
        new Thread(this::initDatabase, "ExpiryDB-Init").start();
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
                            medicine_id INT,
                            medicine_name VARCHAR(255) NOT NULL,
                            batch_number VARCHAR(100) NOT NULL UNIQUE,
                            manufacturing_date DATE NOT NULL,
                            expiry_date DATE NOT NULL,
                            quantity INT NOT NULL DEFAULT 0,
                            unit_price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
                            pharmacy_id VARCHAR(50) NOT NULL DEFAULT 'PHARM-001',
                            status VARCHAR(50) NOT NULL DEFAULT 'AVAILABLE',
                            INDEX idx_expiry (expiry_date),
                            INDEX idx_pharmacy (pharmacy_id)
                        )
                    """;
                    st.executeUpdate(ddl);
                    System.out.println("[ExpiryService] Table 'medicine_batch' verified/initialized.");
                    tablesInitialized = true;
                }
            }
        } catch (Exception e) {
            System.err.println("[ExpiryService] DB table initialization error: " + e.getMessage());
        }
    }

    private synchronized void ensureSeedData() {
        // Check if database has batches
        List<MedicineBatch> existing = fetchBatchesFromDB("PHARM-001");
        if (existing.isEmpty()) {
            System.out.println("[ExpiryService] Seeding initial medicine batches for evaluation...");
            LocalDate today = LocalDate.now();

            List<MedicineBatch> seeds = List.of(
                    // Critical near expiry (within 10-14 days)
                    new MedicineBatch(0, 1, "Paracetamol 650mg", "BAT-PCM-091",
                            today.minusMonths(11), today.plusDays(10), 120, 18.50, "PHARM-001"),

                    // Expiring soon (within 24 days)
                    new MedicineBatch(0, 2, "Amoxicillin 500mg", "BAT-AMX-204",
                            today.minusMonths(18), today.plusDays(24), 85, 42.00, "PHARM-001"),

                    // Expired (expired 5 days ago)
                    new MedicineBatch(0, 3, "Azithromycin 500mg", "BAT-AZI-118",
                            today.minusMonths(24), today.minusDays(5), 40, 65.00, "PHARM-001"),

                    // Critical (within 14 days)
                    new MedicineBatch(0, 4, "Ciprofloxacin 500mg", "BAT-CIP-302",
                            today.minusMonths(15), today.plusDays(14), 60, 38.00, "PHARM-001"),

                    // Attention (within 45 days)
                    new MedicineBatch(0, 5, "Pantoprazole 40mg", "BAT-PAN-841",
                            today.minusMonths(10), today.plusDays(45), 200, 28.00, "PHARM-001"),

                    // Attention (within 55 days)
                    new MedicineBatch(0, 6, "Metformin 500mg SR", "BAT-MET-554",
                            today.minusMonths(12), today.plusDays(55), 150, 15.00, "PHARM-001"),

                    // Safe batches (6+ months)
                    new MedicineBatch(0, 7, "Atorvastatin 10mg", "BAT-ATO-712",
                            today.minusMonths(6), today.plusMonths(8), 180, 52.00, "PHARM-001"),

                    new MedicineBatch(0, 8, "Cetirizine 10mg", "BAT-CET-409",
                            today.minusMonths(4), today.plusMonths(12), 300, 8.50, "PHARM-001"),

                    new MedicineBatch(0, 9, "Ibuprofen 400mg", "BAT-IBU-619",
                            today.minusMonths(5), today.plusMonths(10), 220, 12.00, "PHARM-001"),

                    new MedicineBatch(0, 10, "Budesonide Inhaler", "BAT-BUD-103",
                            today.minusMonths(8), today.plusMonths(14), 45, 110.00, "PHARM-001")
            );

            for (MedicineBatch batch : seeds) {
                saveBatch(batch);
            }
        }
        if (!inMemoryBatches.isEmpty()) return;

        LocalDate today = LocalDate.now();

        List<MedicineBatch> seeds = List.of(
                // Critical near expiry (within 10-14 days)
                new MedicineBatch(1, 1, "Paracetamol 650mg", "BAT-PCM-091",
                        today.minusMonths(11), today.plusDays(10), 120, 18.50, "PHARM-001"),

                // Expiring soon (within 24 days)
                new MedicineBatch(2, 2, "Amoxicillin 500mg", "BAT-AMX-204",
                        today.minusMonths(18), today.plusDays(24), 85, 42.00, "PHARM-001"),

                // Expired (expired 5 days ago)
                new MedicineBatch(3, 3, "Azithromycin 500mg", "BAT-AZI-118",
                        today.minusMonths(24), today.minusDays(5), 40, 65.00, "PHARM-001"),

                // Critical (within 14 days)
                new MedicineBatch(4, 4, "Ciprofloxacin 500mg", "BAT-CIP-302",
                        today.minusMonths(15), today.plusDays(14), 60, 38.00, "PHARM-001"),

                // Attention (within 45 days)
                new MedicineBatch(5, 5, "Pantoprazole 40mg", "BAT-PAN-841",
                        today.minusMonths(10), today.plusDays(45), 200, 28.00, "PHARM-001"),

                // Attention (within 55 days)
                new MedicineBatch(6, 6, "Metformin 500mg SR", "BAT-MET-554",
                        today.minusMonths(12), today.plusDays(55), 150, 15.00, "PHARM-001"),

                // Safe batches (6+ months)
                new MedicineBatch(7, 7, "Atorvastatin 10mg", "BAT-ATO-712",
                        today.minusMonths(6), today.plusMonths(8), 180, 52.00, "PHARM-001"),

                new MedicineBatch(8, 8, "Cetirizine 10mg", "BAT-CET-409",
                        today.minusMonths(4), today.plusMonths(12), 300, 8.50, "PHARM-001"),

                new MedicineBatch(9, 9, "Ibuprofen 400mg", "BAT-IBU-619",
                        today.minusMonths(5), today.plusMonths(10), 220, 12.00, "PHARM-001"),

                new MedicineBatch(10, 10, "Budesonide Inhaler", "BAT-BUD-103",
                        today.minusMonths(8), today.plusMonths(14), 45, 110.00, "PHARM-001")
        );

        // Populate in-memory store first so UI works immediately in evaluation/offline mode
        inMemoryBatches.addAll(seeds);

        // If DB is reachable, sync seeds in background without blocking UI
        new Thread(() -> {
            try (Connection con = DBConnection.connectToMySql()) {
                if (con != null) {
                    for (MedicineBatch batch : seeds) {
                        try {
                            String sql = """
                                INSERT INTO medicine_batch (medicine_id, medicine_name, batch_number, manufacturing_date, expiry_date, quantity, unit_price, pharmacy_id, status)
                                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                                ON DUPLICATE KEY UPDATE quantity = VALUES(quantity)
                            """;
                            try (PreparedStatement ps = con.prepareStatement(sql)) {
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
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }, "ExpirySeed-Sync").start();
    }

    public synchronized MedicineBatch saveBatch(MedicineBatch batch) {
        if (batch == null) return null;
        inMemoryBatches.removeIf(b -> b.getBatchNumber().equalsIgnoreCase(batch.getBatchNumber()));
        inMemoryBatches.add(batch);

        try (Connection con = DBConnection.connectToMySql()) {
            if (con != null) {
                String sql = """
                    INSERT INTO medicine_batch (medicine_id, medicine_name, batch_number, manufacturing_date, expiry_date, quantity, unit_price, pharmacy_id, status)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                        quantity = VALUES(quantity),
                        unit_price = VALUES(unit_price),
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
                }
            }
        } catch (Exception e) {
            System.err.println("[ExpiryService] Error saving batch to DB: " + e.getMessage());
        }
        new Thread(() -> {
            try (Connection con = DBConnection.connectToMySql()) {
                if (con != null) {
                    String sql = """
                        INSERT INTO medicine_batch (medicine_id, medicine_name, batch_number, manufacturing_date, expiry_date, quantity, unit_price, pharmacy_id, status)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                            quantity = VALUES(quantity),
                            unit_price = VALUES(unit_price),
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
                    }
                }
            } catch (Exception e) {
                System.err.println("[ExpiryService] Error saving batch to DB: " + e.getMessage());
            }
        }, "SaveBatch-Worker").start();
        return batch;
    }

    public List<MedicineBatch> fetchBatchesFromDB(String pharmacyId) {
        List<MedicineBatch> list = new ArrayList<>();
        try (Connection con = DBConnection.connectToMySql()) {
            if (con != null) {
                String sql = "SELECT * FROM medicine_batch WHERE pharmacy_id = ? OR pharmacy_id = 'ALL' ORDER BY expiry_date ASC";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, pharmacyId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            MedicineBatch b = new MedicineBatch(
                                    rs.getInt("batch_id"),
                                    rs.getInt("medicine_id"),
                                    rs.getString("medicine_name"),
                                    rs.getString("batch_number"),
                                    rs.getDate("manufacturing_date").toLocalDate(),
                                    rs.getDate("expiry_date").toLocalDate(),
                                    rs.getInt("quantity"),
                                    rs.getDouble("unit_price"),
                                    rs.getString("pharmacy_id")
                            );
                            list.add(b);
                        }
                    }
                }
                return list;
                if (!list.isEmpty()) {
                    return list;
                }
            }
        } catch (Exception e) {
            System.err.println("[ExpiryService] DB fetch failed, using memory cache: " + e.getMessage());
        }

        // Fallback to in-memory batches
        return new ArrayList<>(inMemoryBatches);
    }

    /**
     * Checks all medicine batches for expiration status, calculates remaining days,
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
        List<MedicineBatch> batches = new ArrayList<>(inMemoryBatches);
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

    public void disposeBatch(String batchNumber) {
        inMemoryBatches.removeIf(b -> b.getBatchNumber().equalsIgnoreCase(batchNumber));
        try (Connection con = DBConnection.connectToMySql()) {
            if (con != null) {
                String sql = "DELETE FROM medicine_batch WHERE batch_number = ?";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, batchNumber);
                    ps.executeUpdate();
                }
            }
        } catch (Exception e) {
            System.err.println("[ExpiryService] Error disposing batch: " + e.getMessage());
        }
        new Thread(() -> {
            try (Connection con = DBConnection.connectToMySql()) {
                if (con != null) {
                    String sql = "DELETE FROM medicine_batch WHERE batch_number = ?";
                    try (PreparedStatement ps = con.prepareStatement(sql)) {
                        ps.setString(1, batchNumber);
                        ps.executeUpdate();
                    }
                }
            } catch (Exception e) {
                System.err.println("[ExpiryService] Error disposing batch: " + e.getMessage());
            }
        }, "DisposeBatch-Worker").start();
    }

    public void updateBatchExpiry(String batchNumber, LocalDate newDate) {
        for (MedicineBatch b : inMemoryBatches) {
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
                }
            }
        } catch (Exception e) {
            System.err.println("[ExpiryService] Error updating batch expiry: " + e.getMessage());
        }
        new Thread(() -> {
            try (Connection con = DBConnection.connectToMySql()) {
                if (con != null) {
                    String sql = "UPDATE medicine_batch SET expiry_date = ? WHERE batch_number = ?";
                    try (PreparedStatement ps = con.prepareStatement(sql)) {
                        ps.setDate(1, java.sql.Date.valueOf(newDate));
                        ps.setString(2, batchNumber);
                        ps.executeUpdate();
                    }
                }
            } catch (Exception e) {
                System.err.println("[ExpiryService] Error updating batch expiry: " + e.getMessage());
            }
        }, "UpdateExpiry-Worker").start();
    }
}
