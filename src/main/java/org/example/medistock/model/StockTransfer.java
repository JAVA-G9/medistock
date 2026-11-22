package org.example.medistock.model;

import java.time.LocalDateTime;

/**
 * Represents a physical/operational transfer of medicine stock between pharmacies.
 * Owned by Member 3 (Stock Exchange & Requests).
 */
public class StockTransfer {

    public enum TransferStatus {
        INITIATED,
        DISPATCHED,
        DELIVERED,
        VERIFIED,
        CANCELLED
    }

    private String transferId;
    private String requestId;
    private String listingId;
    private String sourcePharmacyId;
    private String sourcePharmacyName;
    private String destinationPharmacyId;
    private String destinationPharmacyName;
    private String medicineName;
    private String batchNumber;
    private int quantity;
    private double totalPrice;
    private TransferStatus status;
    private String trackingNumber;
    private LocalDateTime createdAt;
    private LocalDateTime dispatchedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime verifiedAt;

    public StockTransfer(String transferId, String requestId, String listingId,
                         String sourcePharmacyId, String sourcePharmacyName,
                         String destinationPharmacyId, String destinationPharmacyName,
                         String medicineName, String batchNumber, int quantity, double totalPrice) {
        this.transferId = transferId;
        this.requestId = requestId;
        this.listingId = listingId;
        this.sourcePharmacyId = sourcePharmacyId;
        this.sourcePharmacyName = sourcePharmacyName;
        this.destinationPharmacyId = destinationPharmacyId;
        this.destinationPharmacyName = destinationPharmacyName;
        this.medicineName = medicineName;
        this.batchNumber = batchNumber;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.status = TransferStatus.INITIATED;
        this.createdAt = LocalDateTime.now();
        this.trackingNumber = "TRK-" + (System.currentTimeMillis() % 1000000);
    }

    public String getTransferId() {
        return transferId;
    }

    public void setTransferId(String transferId) {
        this.transferId = transferId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getListingId() {
        return listingId;
    }

    public void setListingId(String listingId) {
        this.listingId = listingId;
    }

    public String getSourcePharmacyId() {
        return sourcePharmacyId;
    }

    public void setSourcePharmacyId(String sourcePharmacyId) {
        this.sourcePharmacyId = sourcePharmacyId;
    }

    public String getSourcePharmacyName() {
        return sourcePharmacyName;
    }

    public void setSourcePharmacyName(String sourcePharmacyName) {
        this.sourcePharmacyName = sourcePharmacyName;
    }

    public String getDestinationPharmacyId() {
        return destinationPharmacyId;
    }

    public void setDestinationPharmacyId(String destinationPharmacyId) {
        this.destinationPharmacyId = destinationPharmacyId;
    }

    public String getDestinationPharmacyName() {
        return destinationPharmacyName;
    }

    public void setDestinationPharmacyName(String destinationPharmacyName) {
        this.destinationPharmacyName = destinationPharmacyName;
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

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public void setStatus(TransferStatus status) {
        this.status = status;
        if (status == TransferStatus.DISPATCHED && dispatchedAt == null) {
            dispatchedAt = LocalDateTime.now();
        } else if (status == TransferStatus.DELIVERED && deliveredAt == null) {
            deliveredAt = LocalDateTime.now();
        } else if (status == TransferStatus.VERIFIED && verifiedAt == null) {
            verifiedAt = LocalDateTime.now();
        }
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getDispatchedAt() {
        return dispatchedAt;
    }

    public void setDispatchedAt(LocalDateTime dispatchedAt) {
        this.dispatchedAt = dispatchedAt;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    @Override
    public String toString() {
        return String.format("[%s] Transfer of %d x %s from %s to %s [%s]",
                transferId, quantity, medicineName, sourcePharmacyName, destinationPharmacyName, status);
    }
}
