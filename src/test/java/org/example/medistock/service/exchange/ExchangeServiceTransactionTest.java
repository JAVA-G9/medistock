package org.example.medistock.service.exchange;

import org.example.medistock.concurrency.EventProducer;
import org.example.medistock.model.ExchangeListing;
import org.example.medistock.model.ExchangeRequest;
import org.example.medistock.model.StockTransfer;
import org.example.medistock.repository.exchange.InMemoryExchangeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests validating that stock reservations, request rejections,
 * cancellations, and transfer completions correctly maintain available inventory.
 * Owned by Member 3 (Stock Exchange & Requests).
 */
public class ExchangeServiceTransactionTest {

    private ExchangeService exchangeService;
    private final String SELLER_ID = "PHARM-TEST-001";
    private final String SELLER_NAME = "Apex Care Metro";
    private final String BUYER_ID = "PHARM-TEST-002";
    private final String BUYER_NAME = "City Health Clinic";

    @BeforeEach
    public void setUp() {
        exchangeService = new ExchangeService(new InMemoryExchangeRepository(), new EventProducer());
    }

    @Test
    @DisplayName("Rejecting an incoming request must release reserved stock back to available inventory")
    public void testRejectRequestRestoresAvailableQuantity() {
        ExchangeListing listing = exchangeService.createListing(
                SELLER_ID, SELLER_NAME, "Azithromycin 500mg", "AZI-TX-01",
                LocalDate.now().plusMonths(6), 100, 50.0, "Batch for testing"
        );

        assertEquals(100, listing.getTotalQuantity());
        assertEquals(0, listing.getReservedQuantity());
        assertEquals(100, listing.getAvailableQuantity());
        assertEquals(ExchangeListing.ListingStatus.AVAILABLE, listing.getStatus());

        // Submit request for 30 units
        ExchangeRequest request = exchangeService.submitRequest(
                listing.getListingId(), BUYER_ID, BUYER_NAME, 30, 50.0, "Urgent need"
        );

        assertEquals(30, listing.getReservedQuantity());
        assertEquals(70, listing.getAvailableQuantity());
        assertEquals(ExchangeRequest.RequestStatus.PENDING, request.getStatus());

        // Reject request
        exchangeService.rejectRequest(request.getRequestId(), SELLER_ID, "Price mismatch");

        assertEquals(0, listing.getReservedQuantity(), "Reserved quantity must return to 0");
        assertEquals(100, listing.getAvailableQuantity(), "Available quantity must return to 100");
        assertEquals(ExchangeListing.ListingStatus.AVAILABLE, listing.getStatus());
        assertEquals(ExchangeRequest.RequestStatus.REJECTED, request.getStatus());
    }

    @Test
    @DisplayName("Cancelling an outgoing request must release reserved stock back to seller")
    public void testCancelOutgoingRequestRestoresAvailableQuantity() {
        ExchangeListing listing = exchangeService.createListing(
                SELLER_ID, SELLER_NAME, "Paracetamol 650mg", "PCM-TX-02",
                LocalDate.now().plusMonths(8), 200, 25.0, "Surplus test stock"
        );

        ExchangeRequest request = exchangeService.submitRequest(
                listing.getListingId(), BUYER_ID, BUYER_NAME, 50, 25.0, "Bulk purchase"
        );

        assertEquals(50, listing.getReservedQuantity());
        assertEquals(150, listing.getAvailableQuantity());

        // Requester cancels the outgoing request
        exchangeService.cancelRequest(request.getRequestId(), BUYER_ID);

        assertEquals(0, listing.getReservedQuantity(), "Reserved quantity must return to 0 after cancellation");
        assertEquals(200, listing.getAvailableQuantity(), "Available quantity must be restored to 200");
        assertEquals(ExchangeRequest.RequestStatus.CANCELLED, request.getStatus());
    }

    @Test
    @DisplayName("Fully reserving a listing sets status to RESERVED; rejection restores status to AVAILABLE")
    public void testFullReservationAndReleaseRestoresAvailableStatus() {
        ExchangeListing listing = exchangeService.createListing(
                SELLER_ID, SELLER_NAME, "Augmentin 625 Duo", "AUG-TX-03",
                LocalDate.now().plusMonths(4), 50, 150.0, "Limited supply"
        );

        // Reserve 100% of available stock
        ExchangeRequest request = exchangeService.submitRequest(
                listing.getListingId(), BUYER_ID, BUYER_NAME, 50, 150.0, "Entire batch request"
        );

        assertEquals(50, listing.getReservedQuantity());
        assertEquals(0, listing.getAvailableQuantity());
        assertEquals(ExchangeListing.ListingStatus.RESERVED, listing.getStatus());

        // Reject request
        exchangeService.rejectRequest(request.getRequestId(), SELLER_ID, "Facility policy");

        assertEquals(0, listing.getReservedQuantity());
        assertEquals(50, listing.getAvailableQuantity());
        assertEquals(ExchangeListing.ListingStatus.AVAILABLE, listing.getStatus(), "Status must revert to AVAILABLE");
    }

    @Test
    @DisplayName("Approved transfer completion permanently deducts total and reserved stock")
    public void testApproveAndCompleteTransferPermanentlyDeductsStock() {
        ExchangeListing listing = exchangeService.createListing(
                SELLER_ID, SELLER_NAME, "Metformin 500mg SR", "MET-TX-04",
                LocalDate.now().plusMonths(5), 100, 40.0, "Chronic care batch"
        );

        ExchangeRequest request = exchangeService.submitRequest(
                listing.getListingId(), BUYER_ID, BUYER_NAME, 40, 40.0, "Clinical stock"
        );

        StockTransfer transfer = exchangeService.approveRequest(request.getRequestId(), SELLER_ID);
        assertNotNull(transfer);
        assertEquals(StockTransfer.TransferStatus.INITIATED, transfer.getStatus());
        assertEquals(ExchangeRequest.RequestStatus.APPROVED, request.getStatus());

        // Verify and complete transfer
        exchangeService.updateTransferStatus(transfer.getTransferId(), StockTransfer.TransferStatus.VERIFIED);

        assertEquals(60, listing.getTotalQuantity(), "Total quantity must permanently decrease to 60");
        assertEquals(0, listing.getReservedQuantity(), "Reserved quantity must be cleared");
        assertEquals(60, listing.getAvailableQuantity(), "Available quantity must remain 60");
        assertEquals(ExchangeListing.ListingStatus.AVAILABLE, listing.getStatus());
    }
}
