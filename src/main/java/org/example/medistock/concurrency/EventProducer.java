package org.example.medistock.concurrency;

import org.example.medistock.model.AppEvent;
import org.example.medistock.model.ExchangeListing;
import org.example.medistock.model.ExchangeRequest;
import org.example.medistock.model.StockTransfer;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Event Producer owned by Member 3 (Stock Exchange & Requests).
 * Responsible for creating AppEvent objects when stock listings, requests,
 * reservations, and transfers occur, and pushing them to the shared CustomEventQueue.
 */
public class EventProducer {

    private final CustomEventQueue eventQueue;
    private final ExecutorService producerExecutor;

    public EventProducer() {
        this(CustomEventQueue.getInstance());
    }

    public EventProducer(CustomEventQueue eventQueue) {
        this.eventQueue = eventQueue;
        this.producerExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "EventProducer-Thread");
            t.setDaemon(true); // Daemon thread: does not block JVM shutdown
            return t;
        });
    }

    /**
     * Publishes an event to the queue asynchronously so caller threads are never blocked.
     */
    public void publishEvent(AppEvent event) {
        producerExecutor.submit(() -> {
            try {
                System.out.printf("[PRODUCER] [%s] Submitting event %s to queue (Current Queue Size: %d)%n",
                        Thread.currentThread().getName(), event.getEventType(), eventQueue.size());
                eventQueue.put(event);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("[PRODUCER] Interrupted while publishing event: " + e.getMessage());
            }
        });
    }

    public void publishListingCreated(ExchangeListing listing) {
        AppEvent event = new AppEvent(
                "EVT-" + UUID.randomUUID().toString().substring(0, 8),
                AppEvent.EventType.LISTING_CREATED,
                listing.getSellerPharmacyId(),
                "ALL",
                listing.getListingId(),
                String.format("Pharmacy '%s' published listing for %d units of %s (Batch: %s)",
                        listing.getSellerPharmacyName(), listing.getTotalQuantity(),
                        listing.getMedicineName(), listing.getBatchNumber())
        );
        publishEvent(event);
    }

    public void publishListingCancelled(ExchangeListing listing) {
        AppEvent event = new AppEvent(
                "EVT-" + UUID.randomUUID().toString().substring(0, 8),
                AppEvent.EventType.LISTING_CANCELLED,
                listing.getSellerPharmacyId(),
                "ALL",
                listing.getListingId(),
                String.format("Listing for %s (Batch: %s) was cancelled by seller.",
                        listing.getMedicineName(), listing.getBatchNumber())
        );
        publishEvent(event);
    }

    public void publishRequestSubmitted(ExchangeRequest request) {
        AppEvent event = new AppEvent(
                "EVT-" + UUID.randomUUID().toString().substring(0, 8),
                AppEvent.EventType.REQUEST_SUBMITTED,
                request.getRequesterPharmacyId(),
                request.getSellerPharmacyId(),
                request.getRequestId(),
                String.format("Pharmacy '%s' requested %d units of %s from your listing %s",
                        request.getRequesterPharmacyName(), request.getRequestedQuantity(),
                        request.getMedicineName(), request.getListingId())
        );
        publishEvent(event);
    }

    public void publishStockReserved(String listingId, String requesterId, String sellerId, int quantity, String medicineName) {
        AppEvent event = new AppEvent(
                "EVT-" + UUID.randomUUID().toString().substring(0, 8),
                AppEvent.EventType.STOCK_RESERVED,
                sellerId,
                requesterId,
                listingId,
                String.format("Reserved %d units of %s for pharmacy '%s'",
                        quantity, medicineName, requesterId)
        );
        publishEvent(event);
    }

    public void publishStockReleased(String listingId, String requesterId, String sellerId, int quantity, String medicineName) {
        AppEvent event = new AppEvent(
                "EVT-" + UUID.randomUUID().toString().substring(0, 8),
                AppEvent.EventType.STOCK_RELEASED,
                sellerId,
                requesterId,
                listingId,
                String.format("Released %d reserved units of %s back to available stock",
                        quantity, medicineName)
        );
        publishEvent(event);
    }

    public void publishRequestApproved(ExchangeRequest request) {
        AppEvent event = new AppEvent(
                "EVT-" + UUID.randomUUID().toString().substring(0, 8),
                AppEvent.EventType.REQUEST_APPROVED,
                request.getSellerPharmacyId(),
                request.getRequesterPharmacyId(),
                request.getRequestId(),
                String.format("Request %s for %d units of %s was APPROVED by seller.",
                        request.getRequestId(), request.getRequestedQuantity(), request.getMedicineName())
        );
        publishEvent(event);
    }

    public void publishRequestRejected(ExchangeRequest request, String reason) {
        AppEvent event = new AppEvent(
                "EVT-" + UUID.randomUUID().toString().substring(0, 8),
                AppEvent.EventType.REQUEST_REJECTED,
                request.getSellerPharmacyId(),
                request.getRequesterPharmacyId(),
                request.getRequestId(),
                String.format("Request %s for %s was REJECTED: %s",
                        request.getRequestId(), request.getMedicineName(), reason)
        );
        publishEvent(event);
    }

    public void publishTransferDispatched(StockTransfer transfer) {
        AppEvent event = new AppEvent(
                "EVT-" + UUID.randomUUID().toString().substring(0, 8),
                AppEvent.EventType.TRANSFER_DISPATCHED,
                transfer.getSourcePharmacyId(),
                transfer.getDestinationPharmacyId(),
                transfer.getTransferId(),
                String.format("Transfer %s: %d units of %s dispatched (Tracking: %s)",
                        transfer.getTransferId(), transfer.getQuantity(),
                        transfer.getMedicineName(), transfer.getTrackingNumber())
        );
        publishEvent(event);
    }

    public void publishTransferCompleted(StockTransfer transfer) {
        AppEvent event = new AppEvent(
                "EVT-" + UUID.randomUUID().toString().substring(0, 8),
                AppEvent.EventType.TRANSFER_COMPLETED,
                transfer.getSourcePharmacyId(),
                transfer.getDestinationPharmacyId(),
                transfer.getTransferId(),
                String.format("Transfer %s: %d units of %s verified and completed at destination.",
                        transfer.getTransferId(), transfer.getQuantity(), transfer.getMedicineName())
        );
        publishEvent(event);
    }

    public void shutdown() {
        producerExecutor.shutdown();
    }
}
