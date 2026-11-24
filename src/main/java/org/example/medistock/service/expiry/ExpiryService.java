
package org.example.medistock.service.expiry;

import org.example.medistock.model.ExpiryRecord;
import org.example.medistock.model.MedicineBatch;
import org.example.medistock.model.Notification;
import org.example.medistock.service.notifications.NotificationService;
import org.example.medistock.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service for medicine batch expiration tracking,
 * risk classification and expiry notifications.
 *
 * Owned by Member 4 (Expiry, Notifications & Reports).
 *
 * Uses the existing medicine_batch table managed by
 * the inventory module.
 */
public class ExpiryService {

    private static ExpiryService instance;

    private final NotificationService notificationService;

    private final List<MedicineBatch> inMemoryBatches =
            new CopyOnWriteArrayList<>();

    private final Set<String> notifiedBatchKeys =
            ConcurrentHashMap.newKeySet();

    public ExpiryService() {
        this(NotificationService.getInstance());
    }

    public ExpiryService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public static synchronized ExpiryService getInstance() {
        if (instance == null) {
            instance = new ExpiryService();
        }

        return instance;
    }

    /**
     * Saves a batch using the existing inventory table structure.
     */
    public synchronized MedicineBatch saveBatch(MedicineBatch batch) {

        if (batch == null) {
            return null;
        }

        String sql = """
                INSERT INTO medicine_batch
                (medicine_id, medicine_name, batch_number,
                 manufacturing_date, expiry_date,
                 quantity_received, quantity_available,
                 purchase_price, pharmacy_id, status, received_date)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    medicine_name = VALUES(medicine_name),
                    manufacturing_date = VALUES(manufacturing_date),
                    expiry_date = VALUES(expiry_date),
                    quantity_received = VALUES(quantity_received),
                    quantity_available = VALUES(quantity_available),
                    purchase_price = VALUES(purchase_price),
                    status = VALUES(status)
                """;

        try (Connection con = DBConnection.connectToMySql()) {

            if (con == null) {
                System.err.println("[ExpiryService] Database connection unavailable.");
                return null;
            }

            try (PreparedStatement ps = con.prepareStatement(
                    sql, Statement.RETURN_GENERATED_KEYS)) {

                ps.setInt(1, batch.getMedicineId());
                ps.setString(2, batch.getMedicineName());
                ps.setString(3, batch.getBatchNumber());
                ps.setDate(4, java.sql.Date.valueOf(batch.getManufacturingDate()));
                ps.setDate(5, java.sql.Date.valueOf(batch.getExpiryDate()));
                ps.setInt(6, batch.getQuantity());
                ps.setInt(7, batch.getQuantity());
                ps.setDouble(8, batch.getUnitPrice());
                ps.setString(9, batch.getPharmacyId());
                ps.setString(10, "AVAILABLE");
                ps.setDate(11, java.sql.Date.valueOf(LocalDate.now()));

                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        batch.setBatchId(rs.getInt(1));
                    }
                }
            }

            inMemoryBatches.removeIf(
                    b -> b.getBatchNumber().equalsIgnoreCase(batch.getBatchNumber())
            );

            inMemoryBatches.add(batch);

            return batch;

        } catch (Exception e) {
            System.err.println(
                    "[ExpiryService] Error saving batch: " + e.getMessage()
            );
            return null;
        }
    }

    /**
     * Fetches batches belonging to the requested pharmacy.
     * Falls back to the in-memory list if the database is unavailable.
     */
    public List<MedicineBatch> fetchBatchesFromDB(String pharmacyId) {

        List<MedicineBatch> batches = new ArrayList<>();

        String sql = """
                SELECT batch_id, medicine_id, medicine_name,
                       batch_number, manufacturing_date, expiry_date,
                       quantity_available, purchase_price, pharmacy_id
                FROM medicine_batch
                WHERE pharmacy_id = ? OR pharmacy_id = 'ALL'
                ORDER BY expiry_date ASC
                """;

        try (Connection con = DBConnection.connectToMySql()) {

            if (con == null) {
                return new ArrayList<>(inMemoryBatches);
            }

            try (PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setString(1, pharmacyId);

                try (ResultSet rs = ps.executeQuery()) {

                    while (rs.next()) {

                        MedicineBatch batch = new MedicineBatch(
                                rs.getInt("batch_id"),
                                rs.getInt("medicine_id"),
                                rs.getString("medicine_name"),
                                rs.getString("batch_number"),
                                rs.getDate("manufacturing_date").toLocalDate(),
                                rs.getDate("expiry_date").toLocalDate(),
                                rs.getInt("quantity_available"),
                                rs.getDouble("purchase_price"),
                                rs.getString("pharmacy_id")
                        );

                        batches.add(batch);
                    }
                }
            }

            return batches;

        } catch (Exception e) {

            System.err.println(
                    "[ExpiryService] Database fetch failed: " + e.getMessage()
            );

            return new ArrayList<>(inMemoryBatches);
        }
    }

    /**
     * Checks batches and classifies them according to expiry date.
     * Returns only urgent records for the monitoring thread.
     */
    public synchronized List<ExpiryRecord> checkAndProcessExpiries(
            String pharmacyId) {

        List<MedicineBatch> batches = fetchBatchesFromDB(pharmacyId);

        List<ExpiryRecord> urgentRecords = new ArrayList<>();

        LocalDate today = LocalDate.now();

        for (MedicineBatch batch : batches) {

            long days = ChronoUnit.DAYS.between(
                    today, batch.getExpiryDate()
            );

            ExpiryRecord.ExpiryStatus status = calculateStatus(days);

            ExpiryRecord record = new ExpiryRecord(
                    "EXP-" + batch.getBatchNumber(),
                    batch.getBatchId(),
                    batch.getMedicineName(),
                    batch.getBatchNumber(),
                    batch.getExpiryDate(),
                    days,
                    batch.getQuantity(),
                    batch.getUnitPrice(),
                    batch.getPharmacyId(),
                    status
            );

            if (record.isUrgent()) {

                urgentRecords.add(record);

                triggerNotificationIfNew(record);
            }
        }

        return urgentRecords;
    }

    private ExpiryRecord.ExpiryStatus calculateStatus(long days) {

        if (days < 0) {
            return ExpiryRecord.ExpiryStatus.EXPIRED;
        } else if (days <= 15) {
            return ExpiryRecord.ExpiryStatus.CRITICAL;
        } else if (days <= 30) {
            return ExpiryRecord.ExpiryStatus.EXPIRING_SOON;
        } else if (days <= 60) {
            return ExpiryRecord.ExpiryStatus.ATTENTION;
        }

        return ExpiryRecord.ExpiryStatus.SAFE;
    }

    private void triggerNotificationIfNew(ExpiryRecord record) {

        String key = record.getPharmacyId() + ":"
                + record.getBatchNumber() + ":"
                + record.getStatus();

        if (!notifiedBatchKeys.add(key)) {
            return;
        }

        if (notificationService == null) {
            return;
        }

        String title;
        String message;

        Notification.Priority priority;
        Notification.NotificationType type;

        if (record.getStatus() == ExpiryRecord.ExpiryStatus.EXPIRED) {

            title = "EXPIRED BATCH: " + record.getMedicineName();

            message = String.format(
                    "Batch %s expired %d days ago (%d units). "
                            + "Immediately quarantine or dispose.",
                    record.getBatchNumber(),
                    Math.abs(record.getDaysRemaining()),
                    record.getQuantity()
            );

            priority = Notification.Priority.HIGH;
            type = Notification.NotificationType.BATCH_EXPIRED;

        } else if (record.getStatus() == ExpiryRecord.ExpiryStatus.CRITICAL) {

            title = "CRITICAL EXPIRY: " + record.getMedicineName();

            message = String.format(
                    "Batch %s expires in %d days (%d units). "
                            + "Prioritize its use or dispatch.",
                    record.getBatchNumber(),
                    record.getDaysRemaining(),
                    record.getQuantity()
            );

            priority = Notification.Priority.HIGH;
            type = Notification.NotificationType.EXPIRY_WARNING;

        } else {

            title = "Near-Expiry Alert: " + record.getMedicineName();

            message = String.format(
                    "Batch %s expires on %s (%d days remaining).",
                    record.getBatchNumber(),
                    record.getFormattedExpiryDate(),
                    record.getDaysRemaining()
            );

            priority = Notification.Priority.NORMAL;
            type = Notification.NotificationType.EXPIRY_WARNING;
        }

        notificationService.createNotification(
                record.getPharmacyId(),
                title,
                message,
                type,
                priority
        );
    }

    /**
     * Returns all expiry records, including safe batches.
     */
    public List<ExpiryRecord> getAllExpiryRecords(String pharmacyId) {

        List<MedicineBatch> batches = fetchBatchesFromDB(pharmacyId);

        List<ExpiryRecord> records = new ArrayList<>();

        LocalDate today = LocalDate.now();

        for (MedicineBatch batch : batches) {

            long days = ChronoUnit.DAYS.between(
                    today, batch.getExpiryDate()
            );

            ExpiryRecord record = new ExpiryRecord(
                    "EXP-" + batch.getBatchNumber(),
                    batch.getBatchId(),
                    batch.getMedicineName(),
                    batch.getBatchNumber(),
                    batch.getExpiryDate(),
                    days,
                    batch.getQuantity(),
                    batch.getUnitPrice(),
                    batch.getPharmacyId(),
                    calculateStatus(days)
            );

            records.add(record);
        }

        records.sort(
                Comparator.comparingLong(ExpiryRecord::getDaysRemaining)
        );

        return records;
    }

    /**
     * Filters expiry records by medicine name, batch number or status.
     */
    public List<ExpiryRecord> filterRecords(
            String pharmacyId,
            String query,
            String statusFilter) {

        List<ExpiryRecord> all = getAllExpiryRecords(pharmacyId);

        String search = query == null
                ? ""
                : query.trim().toLowerCase();

        return all.stream()
                .filter(record -> {

                    boolean matchesQuery =
                            search.isEmpty()
                                    || record.getMedicineName()
                                    .toLowerCase().contains(search)
                                    || record.getBatchNumber()
                                    .toLowerCase().contains(search);

                    boolean matchesStatus =
                            statusFilter == null
                                    || "ALL".equalsIgnoreCase(statusFilter)
                                    || record.getStatus().name()
                                    .equalsIgnoreCase(statusFilter);

                    return matchesQuery && matchesStatus;
                })
                .toList();
    }

    /**
     * Deletes a batch from the database and memory cache.
     */
    public synchronized void disposeBatch(String batchNumber) {

        String sql =
                "DELETE FROM medicine_batch WHERE batch_number = ?";

        try (Connection con = DBConnection.connectToMySql()) {

            if (con == null) {
                System.err.println("[ExpiryService] Database unavailable.");
                return;
            }

            try (PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setString(1, batchNumber);

                int affected = ps.executeUpdate();

                if (affected > 0) {

                    inMemoryBatches.removeIf(
                            b -> b.getBatchNumber()
                                    .equalsIgnoreCase(batchNumber)
                    );

                    System.out.println(
                            "[ExpiryService] Batch disposed: " + batchNumber
                    );
                }
            }

        } catch (Exception e) {

            System.err.println(
                    "[ExpiryService] Error disposing batch: " + e.getMessage()
            );
        }
    }

    /**
     * Updates the expiry date of an existing batch.
     */
    public synchronized void updateBatchExpiry(
            String batchNumber,
            LocalDate newDate) {

        if (newDate == null) {
            throw new IllegalArgumentException("Expiry date cannot be null.");
        }

        String sql = """
                UPDATE medicine_batch
                SET expiry_date = ?
                WHERE batch_number = ?
                """;

        try (Connection con = DBConnection.connectToMySql()) {

            if (con == null) {
                System.err.println("[ExpiryService] Database unavailable.");
                return;
            }

            try (PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setDate(1, java.sql.Date.valueOf(newDate));
                ps.setString(2, batchNumber);

                int affected = ps.executeUpdate();

                if (affected > 0) {

                    for (MedicineBatch batch : inMemoryBatches) {

                        if (batch.getBatchNumber()
                                .equalsIgnoreCase(batchNumber)) {

                            batch.setExpiryDate(newDate);
                        }
                    }

                    System.out.println(
                            "[ExpiryService] Expiry date updated for "
                                    + batchNumber
                    );
                }
            }

        } catch (Exception e) {

            System.err.println(
                    "[ExpiryService] Error updating expiry: " + e.getMessage()
            );
        }
    }
}
