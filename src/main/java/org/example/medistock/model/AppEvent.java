package org.example.medistock.model;

import java.time.LocalDateTime;

/**
 * Shared event model between Member 3 (Stock Exchange Producer) and Member 4 (Notifications & Expiry Consumer).
 * Produced when exchange, inventory, or reservation state changes, and consumed via CustomEventQueue.
 */
public class AppEvent {

    public enum EventType {
        LISTING_CREATED,
        LISTING_UPDATED,
        LISTING_CANCELLED,
        REQUEST_SUBMITTED,
        STOCK_RESERVED,
        STOCK_RELEASED,
        REQUEST_APPROVED,
        REQUEST_REJECTED,
        TRANSFER_DISPATCHED,
        TRANSFER_COMPLETED,
        TRANSFER_CANCELLED
    }

    private final String eventId;
    private final EventType eventType;
    private final String sourcePharmacyId;
    private final String targetPharmacyId;
    private final String referenceId; // listingId, requestId, or transferId
    private final String message;
    private final LocalDateTime timestamp;

    public AppEvent(String eventId, EventType eventType, String sourcePharmacyId,
                    String targetPharmacyId, String referenceId, String message) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.sourcePharmacyId = sourcePharmacyId;
        this.targetPharmacyId = targetPharmacyId;
        this.referenceId = referenceId;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    public String getEventId() {
        return eventId;
    }

    public EventType getEventType() {
        return eventType;
    }

    public String getSourcePharmacyId() {
        return sourcePharmacyId;
    }

    public String getTargetPharmacyId() {
        return targetPharmacyId;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s | Ref: %s | %s", timestamp, eventType, referenceId, message);
    }
}
