package org.example.medistock.model;

import java.time.LocalDateTime;

/**
 * High-level Exchange domain model representing an initiated or completed stock transaction.
 * Owned by Member 3 (Stock Exchange & Requests).
 */
public class Exchange {

    public enum ExchangeStatus {
        ACTIVE,
        PENDING_CONFIRMATION,
        COMPLETED,
        CANCELLED
    }

    private String exchangeId;
    private String listingId;
    private String requestId;
    private String sourcePharmacyId;
    private String targetPharmacyId;
    private String medicineName;
    private int quantity;
    private double totalAmount;
    private ExchangeStatus status;
    private LocalDateTime timestamp;

    public Exchange() {
        this.timestamp = LocalDateTime.now();
    }

    public Exchange(String exchangeId, String listingId, String requestId,
                    String sourcePharmacyId, String targetPharmacyId,
                    String medicineName, int quantity, double totalAmount,
                    ExchangeStatus status) {
        this.exchangeId = exchangeId;
        this.listingId = listingId;
        this.requestId = requestId;
        this.sourcePharmacyId = sourcePharmacyId;
        this.targetPharmacyId = targetPharmacyId;
        this.medicineName = medicineName;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
        this.status = status;
        this.timestamp = LocalDateTime.now();
    }

    public String getExchangeId() {
        return exchangeId;
    }

    public void setExchangeId(String exchangeId) {
        this.exchangeId = exchangeId;
    }

    public String getListingId() {
        return listingId;
    }

    public void setListingId(String listingId) {
        this.listingId = listingId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getSourcePharmacyId() {
        return sourcePharmacyId;
    }

    public void setSourcePharmacyId(String sourcePharmacyId) {
        this.sourcePharmacyId = sourcePharmacyId;
    }

    public String getTargetPharmacyId() {
        return targetPharmacyId;
    }

    public void setTargetPharmacyId(String targetPharmacyId) {
        this.targetPharmacyId = targetPharmacyId;
    }

    public String getMedicineName() {
        return medicineName;
    }

    public void setMedicineName(String medicineName) {
        this.medicineName = medicineName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public ExchangeStatus getStatus() {
        return status;
    }

    public void setStatus(ExchangeStatus status) {
        this.status = status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
