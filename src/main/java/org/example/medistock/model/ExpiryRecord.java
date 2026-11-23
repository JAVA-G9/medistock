package org.example.medistock.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ExpiryRecord model owned by Member 4 (Expiry, Notifications & Reports).
 * Represents a medicine batch tracked for expiration lifecycle with risk level calculation.
 */
public class ExpiryRecord {

    public enum ExpiryStatus {
        EXPIRED,        // Days <= 0
        CRITICAL,       // Days <= 15
        EXPIRING_SOON,  // Days <= 30
        ATTENTION,      // Days <= 60
        SAFE            // Days > 60
    }

    private String recordId;
    private int batchId;
    private String medicineName;
    private String batchNumber;
    private LocalDate expiryDate;
    private long daysRemaining;
    private int quantity;
    private double unitPrice;
    private String pharmacyId;
    private ExpiryStatus status;
    private LocalDateTime recordedAt;

    public ExpiryRecord() {
        this.recordedAt = LocalDateTime.now();
    }

    public ExpiryRecord(String recordId, int batchId, String medicineName, String batchNumber,
                        LocalDate expiryDate, long daysRemaining, int quantity,
                        double unitPrice, String pharmacyId, ExpiryStatus status) {
        this.recordId = recordId;
        this.batchId = batchId;
        this.medicineName = medicineName;
        this.batchNumber = batchNumber;
        this.expiryDate = expiryDate;
        this.daysRemaining = daysRemaining;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.pharmacyId = pharmacyId;
        this.status = status;
        this.recordedAt = LocalDateTime.now();
    }

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public int getBatchId() {
        return batchId;
    }

    public void setBatchId(int batchId) {
        this.batchId = batchId;
    }

    public String getMedicineName() {
        return medicineName;
    }

    public void setMedicineName(String medicineName) {
        this.medicineName = medicineName;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getFormattedExpiryDate() {
        return expiryDate != null ? expiryDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : "N/A";
    }

    public long getDaysRemaining() {
        return daysRemaining;
    }

    public void setDaysRemaining(long daysRemaining) {
        this.daysRemaining = daysRemaining;
    }

    public String getDaysRemainingDisplay() {
        if (daysRemaining < 0) {
            return Math.abs(daysRemaining) + " days ago";
        } else if (daysRemaining == 0) {
            return "Expires Today";
        } else {
            return daysRemaining + " days left";
        }
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public double getTotalValue() {
        return quantity * unitPrice;
    }

    public String getFormattedTotalValue() {
        return String.format("$%.2f", getTotalValue());
    }

    public String getPharmacyId() {
        return pharmacyId;
    }

    public void setPharmacyId(String pharmacyId) {
        this.pharmacyId = pharmacyId;
    }

    public ExpiryStatus getStatus() {
        return status;
    }

    public void setStatus(ExpiryStatus status) {
        this.status = status;
    }

    public String getStatusDisplayName() {
        return status != null ? status.name().replace('_', ' ') : "UNKNOWN";
    }

    public String getStatusBadgeStyle() {
        if (status == null) return "-fx-background-color: #e2e8f0; -fx-text-fill: #475569;";
        return switch (status) {
            case EXPIRED -> "-fx-background-color: #fee2e2; -fx-text-fill: #991b1b; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 12;";
            case CRITICAL -> "-fx-background-color: #ffedd5; -fx-text-fill: #9a3412; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 12;";
            case EXPIRING_SOON -> "-fx-background-color: #fef3c7; -fx-text-fill: #92400e; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 12;";
            case ATTENTION -> "-fx-background-color: #e0e7ff; -fx-text-fill: #3730a3; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 12;";
            case SAFE -> "-fx-background-color: #d1fae5; -fx-text-fill: #065f46; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 12;";
        };
    }

    public boolean isUrgent() {
        return status == ExpiryStatus.EXPIRED || status == ExpiryStatus.CRITICAL || status == ExpiryStatus.EXPIRING_SOON;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s (Batch: %s) - %s (%d days)", status, medicineName, batchNumber, expiryDate, daysRemaining);
    }
}
