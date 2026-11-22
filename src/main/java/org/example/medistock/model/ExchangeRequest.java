package org.example.medistock.model;

import java.time.LocalDateTime;

/**
 * Represents a request placed by a pharmacy to acquire stock from an exchange listing.
 * Owned by Member 3 (Stock Exchange & Requests).
 */
public class ExchangeRequest {

    public enum RequestStatus {
        PENDING,
        APPROVED,
        REJECTED,
        CANCELLED,
        COMPLETED
    }

    private String requestId;
    private String listingId;
    private String requesterPharmacyId;
    private String requesterPharmacyName;
    private String sellerPharmacyId;
    private String medicineName;
    private int requestedQuantity;
    private double offeredPrice;
    private RequestStatus status;
    private String requestMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ExchangeRequest(String requestId, String listingId, String requesterPharmacyId,
                           String requesterPharmacyName, String sellerPharmacyId,
                           String medicineName, int requestedQuantity, double offeredPrice,
                           String requestMessage) {
        this.requestId = requestId;
        this.listingId = listingId;
        this.requesterPharmacyId = requesterPharmacyId;
        this.requesterPharmacyName = requesterPharmacyName;
        this.sellerPharmacyId = sellerPharmacyId;
        this.medicineName = medicineName;
        this.requestedQuantity = requestedQuantity;
        this.offeredPrice = offeredPrice;
        this.status = RequestStatus.PENDING;
        this.requestMessage = requestMessage;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
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

    public String getRequesterPharmacyId() {
        return requesterPharmacyId;
    }

    public void setRequesterPharmacyId(String requesterPharmacyId) {
        this.requesterPharmacyId = requesterPharmacyId;
    }

    public String getRequesterPharmacyName() {
        return requesterPharmacyName;
    }

    public void setRequesterPharmacyName(String requesterPharmacyName) {
        this.requesterPharmacyName = requesterPharmacyName;
    }

    public String getSellerPharmacyId() {
        return sellerPharmacyId;
    }

    public void setSellerPharmacyId(String sellerPharmacyId) {
        this.sellerPharmacyId = sellerPharmacyId;
    }

    public String getMedicineName() {
        return medicineName;
    }

    public void setMedicineName(String medicineName) {
        this.medicineName = medicineName;
    }

    public int getRequestedQuantity() {
        return requestedQuantity;
    }

    public void setRequestedQuantity(int requestedQuantity) {
        this.requestedQuantity = requestedQuantity;
    }

    public double getOfferedPrice() {
        return offeredPrice;
    }

    public void setOfferedPrice(double offeredPrice) {
        this.offeredPrice = offeredPrice;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public String getRequestMessage() {
        return requestMessage;
    }

    public void setRequestMessage(String requestMessage) {
        this.requestMessage = requestMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public double getTotalAmount() {
        return requestedQuantity * offeredPrice;
    }

    @Override
    public String toString() {
        return String.format("[%s] Req by %s for %d units of %s (@ ₹%.2f) - Status: %s",
                requestId, requesterPharmacyName, requestedQuantity, medicineName, offeredPrice, status);
    }
}
