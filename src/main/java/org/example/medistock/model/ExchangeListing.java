package org.example.medistock.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a surplus medicine batch listed on the marketplace for exchange.
 * Owned by Member 3 (Stock Exchange & Requests).
 */
public class ExchangeListing {

    public enum ListingStatus {
        AVAILABLE,
        RESERVED,
        COMPLETED,
        CANCELLED
    }

    private String listingId;
    private String sellerPharmacyId;
    private String sellerPharmacyName;
    private String medicineName;
    private String batchNumber;
    private LocalDate expiryDate;
    private int totalQuantity;
    private int reservedQuantity;
    private double unitPrice;
    private ListingStatus status;
    private String notes;
    private LocalDateTime createdAt;

    public ExchangeListing(String listingId, String sellerPharmacyId, String sellerPharmacyName,
                           String medicineName, String batchNumber, LocalDate expiryDate,
                           int totalQuantity, double unitPrice, String notes) {
        this.listingId = listingId;
        this.sellerPharmacyId = sellerPharmacyId;
        this.sellerPharmacyName = sellerPharmacyName;
        this.medicineName = medicineName;
        this.batchNumber = batchNumber;
        this.expiryDate = expiryDate;
        this.totalQuantity = totalQuantity;
        this.reservedQuantity = 0;
        this.unitPrice = unitPrice;
        this.status = ListingStatus.AVAILABLE;
        this.notes = notes;
        this.createdAt = LocalDateTime.now();
    }

    public synchronized int getAvailableQuantity() {
        return Math.max(0, totalQuantity - reservedQuantity);
    }

    /**
     * Atomically attempts to reserve quantity on this listing.
     * Prevents race conditions and overselling in concurrent requests.
     */
    public synchronized boolean tryReserve(int quantityToReserve) {
        if (quantityToReserve <= 0) {
            return false;
        }
        if (status != ListingStatus.AVAILABLE) {
            return false;
        }
        if (getAvailableQuantity() >= quantityToReserve) {
            this.reservedQuantity += quantityToReserve;
            if (getAvailableQuantity() == 0) {
                this.status = ListingStatus.RESERVED;
            }
            return true;
        }
        return false;
    }

    /**
     * Releases reserved stock back to available stock (e.g. upon request rejection or cancellation).
     */
    public synchronized void releaseReservation(int quantityToRelease) {
        if (quantityToRelease <= 0) {
            return;
        }
        this.reservedQuantity = Math.max(0, this.reservedQuantity - quantityToRelease);
        if (this.status == ListingStatus.RESERVED && getAvailableQuantity() > 0) {
            this.status = ListingStatus.AVAILABLE;
        }
    }

    /**
     * Permanently deducts stock upon verified transfer completion.
     */
    public synchronized void completeTransfer(int quantityToDeduct) {
        this.totalQuantity = Math.max(0, this.totalQuantity - quantityToDeduct);
        this.reservedQuantity = Math.max(0, this.reservedQuantity - quantityToDeduct);
        if (this.totalQuantity == 0) {
            this.status = ListingStatus.COMPLETED;
        } else if (getAvailableQuantity() > 0) {
            this.status = ListingStatus.AVAILABLE;
        }
    }

    // Standard JavaBeans getters and setters for JavaFX PropertyValueFactory
    public String getListingId() {
        return listingId;
    }

    public void setListingId(String listingId) {
        this.listingId = listingId;
    }

    public String getSellerPharmacyId() {
        return sellerPharmacyId;
    }

    public void setSellerPharmacyId(String sellerPharmacyId) {
        this.sellerPharmacyId = sellerPharmacyId;
    }

    public String getSellerPharmacyName() {
        return sellerPharmacyName;
    }

    public void setSellerPharmacyName(String sellerPharmacyName) {
        this.sellerPharmacyName = sellerPharmacyName;
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

    public synchronized int getTotalQuantity() {
        return totalQuantity;
    }

    public synchronized void setTotalQuantity(int totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public synchronized int getReservedQuantity() {
        return reservedQuantity;
    }

    public synchronized void setReservedQuantity(int reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public synchronized ListingStatus getStatus() {
        return status;
    }

    public synchronized void setStatus(ListingStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s (Batch: %s) Avail: %d/%d @ ₹%.2f [%s]",
                listingId, medicineName, batchNumber, getAvailableQuantity(), totalQuantity, unitPrice, status);
    }
}
