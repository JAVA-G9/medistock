package org.example.medistock.service.exchange;

import org.example.medistock.concurrency.EventProducer;
import org.example.medistock.model.ExchangeListing;
import org.example.medistock.model.ExchangeRequest;
import org.example.medistock.model.StockTransfer;
import org.example.medistock.repository.exchange.ExchangeRepository;
import org.example.medistock.repository.exchange.InMemoryExchangeRepository;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Core business service for Stock Exchange and Requests.
 * Folder: service/exchange/
 * Owned by Member 3 (Stock Exchange & Requests).
 * Coordinates listings, requests, atomic stock reservation, transfers,
 * and event publishing to the shared queue.
 */
public class ExchangeService {

    private static ExchangeService instance;

    private final ExchangeRepository exchangeRepository;
    private final EventProducer eventProducer;
    private final AtomicInteger listingIdGenerator = new AtomicInteger(1000);
    private final AtomicInteger requestIdGenerator = new AtomicInteger(5000);
    private final AtomicInteger transferIdGenerator = new AtomicInteger(8000);

    public ExchangeService() {
        this(new InMemoryExchangeRepository(), new EventProducer());
    }

    public ExchangeService(ExchangeRepository exchangeRepository, EventProducer eventProducer) {
        this.exchangeRepository = exchangeRepository;
        this.eventProducer = eventProducer;
        initSampleData();
    }

    public static synchronized ExchangeService getInstance() {
        if (instance == null) {
            instance = new ExchangeService();
        }
        return instance;
    }

    private org.example.medistock.concurrency.ExchangeWorker exchangeWorker;

    public synchronized void ensureWorkerStarted() {
        if (exchangeWorker == null) {
            exchangeWorker = new org.example.medistock.concurrency.ExchangeWorker(this, 5000);
            exchangeWorker.start();
        }
    }

    private void initSampleData() {
        final String PHARM1_ID = "PHARM-001";
        final String PHARM1_NAME = "Apex Care Pharmacy (Metro City)";
        final String PHARM2_ID = "PHARM-002";
        final String PHARM2_NAME = "City Health Drugstore (Riverdale)";
        final String PHARM3_ID = "PHARM-003";
        final String PHARM3_NAME = "GreenCross LifeSciences (Green Valley)";

        // --- Antibiotics & Anti-Infectives ---
        ExchangeListing l1 = createListing(PHARM1_ID, PHARM1_NAME, "Amoxicillin 500mg",
                "AMX-2024-09", LocalDate.now().plusMonths(3), 120, 85.0, "Surplus stock, 35% discount for bulk exchange");

        ExchangeListing l2 = createListing(PHARM2_ID, PHARM2_NAME, "Azithromycin 500mg",
                "AZI-2024-10", LocalDate.now().plusMonths(2), 70, 115.0, "Near-expiry batch, blister-packed");

        ExchangeListing l3 = createListing(PHARM1_ID, PHARM1_NAME, "Ciprofloxacin 500mg",
                "CIP-2024-11", LocalDate.now().plusMonths(4), 90, 62.0, "Surplus warehouse stock");

        ExchangeListing l4 = createListing(PHARM1_ID, PHARM1_NAME, "Augmentin 625 Duo",
                "AUG-2024-08", LocalDate.now().plusMonths(2), 50, 165.0, "High demand antibiotic, exp in 60 days");

        // --- Analgesics & Antipyretics ---
        ExchangeListing l5 = createListing(PHARM2_ID, PHARM2_NAME, "Paracetamol 650mg (Dolo)",
                "PCM-2024-12", LocalDate.now().plusMonths(5), 350, 28.0, "Overstocked monsoon batch, fast-moving");

        ExchangeListing l6 = createListing(PHARM2_ID, PHARM2_NAME, "Ibuprofen 400mg",
                "IBU-2024-09", LocalDate.now().plusMonths(3), 140, 34.0, "Surplus inventory from seasonal order");

        ExchangeListing l7 = createListing(PHARM1_ID, PHARM1_NAME, "Tramadol 50mg",
                "TRM-2024-11", LocalDate.now().plusMonths(4), 60, 92.0, "Schedule H1 drug, verified chain-of-custody");

        // --- Chronic Care: Cardiac & Diabetes ---
        ExchangeListing l8 = createListing(PHARM1_ID, PHARM1_NAME, "Metformin 500mg SR",
                "MET-2024-10", LocalDate.now().plusMonths(4), 180, 42.0, "Surplus diabetes maintenance stock");

        ExchangeListing l9 = createListing(PHARM2_ID, PHARM2_NAME, "Glimepiride 2mg",
                "GLM-2024-12", LocalDate.now().plusMonths(6), 110, 55.0, "Standard strip packs, verified storage");

        ExchangeListing l10 = createListing(PHARM1_ID, PHARM1_NAME, "Telmisartan 40mg",
                "TEL-2024-09", LocalDate.now().plusMonths(3), 130, 78.0, "Hypertension maintenance surplus");

        ExchangeListing l11 = createListing(PHARM1_ID, PHARM1_NAME, "Atorvastatin 10mg",
                "ATV-2024-11", LocalDate.now().plusMonths(5), 85, 95.0, "Lipid-lowering surplus stock");

        ExchangeListing l12 = createListing(PHARM2_ID, PHARM2_NAME, "Amlodipine 5mg",
                "AML-2024-08", LocalDate.now().plusMonths(2), 160, 32.0, "Near expiry 60-day batch, 40% discount");

        // --- Gastrointestinal & Antacids ---
        ExchangeListing l13 = createListing(PHARM2_ID, PHARM2_NAME, "Pantoprazole 40mg (Pan-40)",
                "PAN-2024-10", LocalDate.now().plusMonths(4), 220, 72.0, "High demand PPI, original carton");

        ExchangeListing l14 = createListing(PHARM1_ID, PHARM1_NAME, "Omeprazole 20mg",
                "OME-2024-09", LocalDate.now().plusMonths(3), 95, 46.0, "Surplus capsules batch");

        ExchangeListing l15 = createListing(PHARM2_ID, PHARM2_NAME, "Ondansetron 4mg",
                "OND-2024-11", LocalDate.now().plusMonths(5), 75, 48.0, "Antiemetic stock, dry storage maintained");

        // --- Respiratory & Antiallergic ---
        // Exactly ONE listing initially published by GreenCross LifeSciences (Green Valley)
        ExchangeListing l16 = createListing(PHARM3_ID, PHARM3_NAME, "Montelukast + Levocetirizine",
                "MLC-2024-09", LocalDate.now().plusMonths(3), 150, 108.0, "Montair-LC equivalent, seasonal surplus from Green Valley");

        ExchangeListing l17 = createListing(PHARM1_ID, PHARM1_NAME, "Cetirizine 10mg",
                "CET-2024-12", LocalDate.now().plusMonths(6), 260, 18.0, "Bulk antihistamine strips");

        ExchangeListing l18 = createListing(PHARM2_ID, PHARM2_NAME, "Budesonide Inhaler 200mcg",
                "BUD-2024-07", LocalDate.now().plusMonths(2), 30, 275.0, "Sealed inhalers, temp-controlled storage");

        // Pre-populate realistic cross-pharmacy exchange interactions
        try {
            // 1. City Health requests 15 units of Augmentin from Apex Care (Pending in Escrow)
            submitRequest(l4.getListingId(), PHARM2_ID, PHARM2_NAME,
                    15, 165.0, "Urgent requirement for local clinic prescription");

            // 2. Apex Care requests 25 units of Montelukast from Green Valley (Pending for Green Valley to Approve/Reject)
            submitRequest(l16.getListingId(), PHARM1_ID, PHARM1_NAME,
                    25, 108.0, "Prescription demand from Metro City outpatient branch");

            // 3. Green Valley requested 50 units of Paracetamol from City Health, which was Approved & Dispatched
            ExchangeRequest req3 = submitRequest(l5.getListingId(), PHARM3_ID, PHARM3_NAME,
                    50, 28.0, "Seasonal restocking for Green Valley dispensary");
            StockTransfer trf1 = approveRequest(req3.getRequestId(), PHARM2_ID);
            updateTransferStatus(trf1.getTransferId(), StockTransfer.TransferStatus.DISPATCHED);
        } catch (Exception ignored) {
        }
    }
    public ExchangeListing createListing(String sellerPharmacyId, String sellerPharmacyName,
                                         String medicineName, String batchNumber,
                                         LocalDate expiryDate, int totalQuantity,
                                         double unitPrice, String notes) {
        if (totalQuantity <= 0) {
            throw new IllegalArgumentException("Total quantity must be greater than zero.");
        }
        if (unitPrice < 0) {
            throw new IllegalArgumentException("Unit price cannot be negative.");
        }

        String listingId = "LST-" + listingIdGenerator.incrementAndGet();
        ExchangeListing listing = new ExchangeListing(
                listingId, sellerPharmacyId, sellerPharmacyName,
                medicineName, batchNumber, expiryDate,
                totalQuantity, unitPrice, notes
        );

        exchangeRepository.saveListing(listing);
        eventProducer.publishListingCreated(listing);

        System.out.printf("[EXCHANGE-SERVICE] Created listing %s for %s (%d units)%n",
                listingId, medicineName, totalQuantity);
        return listing;
    }

    public List<ExchangeListing> getAllActiveListings() {
        return exchangeRepository.findActiveListings();
    }

    public List<ExchangeListing> getAllListings() {
        return exchangeRepository.findAllListings();
    }

    public List<ExchangeListing> getListingsByPharmacy(String pharmacyId) {
        return exchangeRepository.findListingsByPharmacy(pharmacyId);
    }

    public List<ExchangeListing> searchListings(String query) {
        return exchangeRepository.searchListings(query);
    }

    public ExchangeListing getListingById(String listingId) {
        return exchangeRepository.findListingById(listingId);
    }

    public boolean cancelListing(String listingId, String sellerPharmacyId) {
        ExchangeListing listing = exchangeRepository.findListingById(listingId);
        if (listing == null) {
            return false;
        }

        synchronized (listing) {
            if (!listing.getSellerPharmacyId().equalsIgnoreCase(sellerPharmacyId)) {
                throw new IllegalStateException("Only the seller pharmacy can cancel this listing.");
            }
            if (listing.getReservedQuantity() > 0) {
                throw new IllegalStateException("Cannot cancel listing with active reserved quantity. Resolve pending requests first.");
            }
            listing.setStatus(ExchangeListing.ListingStatus.CANCELLED);
            exchangeRepository.updateListing(listing);
            eventProducer.publishListingCancelled(listing);
            return true;
        }
    }

    // ==========================================
    // REQUEST OPERATIONS & CONCURRENT RESERVATION
    // ==========================================

    /**
     * Submits an exchange request for a listing.
     * MULTITHREADING CORE: Uses intrinsic synchronization on the listing instance to coordinate
     * atomic reservation of stock, preventing race conditions and overselling.
     */
    public ExchangeRequest submitRequest(String listingId, String requesterPharmacyId,
                                         String requesterPharmacyName, int requestedQuantity,
                                         double offeredPrice, String message) {
        if (requestedQuantity <= 0) {
            throw new IllegalArgumentException("Requested quantity must be positive.");
        }

        ExchangeListing listing = exchangeRepository.findListingById(listingId);
        if (listing == null) {
            throw new IllegalArgumentException("Listing not found: " + listingId);
        }

        if (listing.getSellerPharmacyId().equalsIgnoreCase(requesterPharmacyId)) {
            throw new IllegalArgumentException("A pharmacy cannot request stock from its own listing.");
        }

        // CRITICAL SECTION: Synchronized on the listing monitor
        synchronized (listing) {
            System.out.printf("[CONCURRENCY] [%s] Attempting to reserve %d units from listing %s (Available: %d)...%n",
                    Thread.currentThread().getName(), requestedQuantity, listingId, listing.getAvailableQuantity());

            boolean reserved = listing.tryReserve(requestedQuantity);
            if (!reserved) {
                System.out.printf("[CONCURRENCY] [%s] REJECTED: Insufficient stock for listing %s (Available: %d, Requested: %d)%n",
                        Thread.currentThread().getName(), listingId, listing.getAvailableQuantity(), requestedQuantity);
                throw new IllegalStateException(String.format(
                        "Cannot reserve %d units. Only %d units available for listing %s.",
                        requestedQuantity, listing.getAvailableQuantity(), listingId));
            }

            // Stock reserved atomically! Persist updated listing state
            exchangeRepository.updateListing(listing);

            String requestId = "REQ-" + requestIdGenerator.incrementAndGet();
            ExchangeRequest request = new ExchangeRequest(
                    requestId, listingId, requesterPharmacyId, requesterPharmacyName,
                    listing.getSellerPharmacyId(), listing.getMedicineName(),
                    requestedQuantity, offeredPrice, message
            );

            exchangeRepository.saveRequest(request);

            // Notify via event producer
            eventProducer.publishStockReserved(listingId, requesterPharmacyId, listing.getSellerPharmacyId(),
                    requestedQuantity, listing.getMedicineName());
            eventProducer.publishRequestSubmitted(request);

            System.out.printf("[CONCURRENCY] [%s] SUCCESS: Reserved %d units. New Available: %d%n",
                    Thread.currentThread().getName(), requestedQuantity, listing.getAvailableQuantity());

            return request;
        }
    }

    /**
     * Approves a pending request, committing reservation and generating a StockTransfer.
     */
    public StockTransfer approveRequest(String requestId, String sellerPharmacyId) {
        ExchangeRequest request = exchangeRepository.findRequestById(requestId);
        if (request == null) {
            throw new IllegalArgumentException("Request not found: " + requestId);
        }

        if (!request.getSellerPharmacyId().equalsIgnoreCase(sellerPharmacyId)) {
            throw new IllegalStateException("Only the seller pharmacy can approve this request.");
        }

        if (request.getStatus() != ExchangeRequest.RequestStatus.PENDING) {
            throw new IllegalStateException("Only PENDING requests can be approved. Current status: " + request.getStatus());
        }

        ExchangeListing listing = exchangeRepository.findListingById(request.getListingId());
        if (listing == null) {
            throw new IllegalStateException("Associated listing not found: " + request.getListingId());
        }

        synchronized (listing) {
            request.setStatus(ExchangeRequest.RequestStatus.APPROVED);
            exchangeRepository.updateRequest(request);

            String transferId = "TRF-" + transferIdGenerator.incrementAndGet();
            StockTransfer transfer = new StockTransfer(
                    transferId, request.getRequestId(), listing.getListingId(),
                    listing.getSellerPharmacyId(), listing.getSellerPharmacyName(),
                    request.getRequesterPharmacyId(), request.getRequesterPharmacyName(),
                    listing.getMedicineName(), listing.getBatchNumber(),
                    request.getRequestedQuantity(), request.getTotalAmount()
            );

            exchangeRepository.saveTransfer(transfer);

            eventProducer.publishRequestApproved(request);
            eventProducer.publishTransferDispatched(transfer);

            System.out.printf("[EXCHANGE-SERVICE] Request %s APPROVED. Initiated Transfer %s.%n",
                    requestId, transferId);
            return transfer;
        }
    }

    /**
     * Rejects a pending request and atomically releases the reserved quantity back to the listing.
     */
    public void rejectRequest(String requestId, String sellerPharmacyId, String reason) {
        ExchangeRequest request = exchangeRepository.findRequestById(requestId);
        if (request == null) {
            throw new IllegalArgumentException("Request not found: " + requestId);
        }

        if (!request.getSellerPharmacyId().equalsIgnoreCase(sellerPharmacyId)) {
            throw new IllegalStateException("Only the seller pharmacy can reject this request.");
        }

        if (request.getStatus() != ExchangeRequest.RequestStatus.PENDING) {
            throw new IllegalStateException("Only PENDING requests can be rejected.");
        }

        ExchangeListing listing = exchangeRepository.findListingById(request.getListingId());
        if (listing == null) {
            throw new IllegalStateException("Associated listing not found: " + request.getListingId());
        }

        synchronized (listing) {
            listing.releaseReservation(request.getRequestedQuantity());
            exchangeRepository.updateListing(listing);

            request.setStatus(ExchangeRequest.RequestStatus.REJECTED);
            exchangeRepository.updateRequest(request);

            eventProducer.publishStockReleased(listing.getListingId(), request.getRequesterPharmacyId(),
                    listing.getSellerPharmacyId(), request.getRequestedQuantity(), listing.getMedicineName());
            eventProducer.publishRequestRejected(request, reason);

            System.out.printf("[EXCHANGE-SERVICE] Request %s REJECTED. Released %d units back to listing %s.%n",
                    requestId, request.getRequestedQuantity(), listing.getListingId());
        }
    }

    /**
     * Cancels an outgoing pending request and releases reserved stock.
     */
    public void cancelRequest(String requestId, String requesterPharmacyId) {
        ExchangeRequest request = exchangeRepository.findRequestById(requestId);
        if (request == null) {
            throw new IllegalArgumentException("Request not found: " + requestId);
        }

        if (!request.getRequesterPharmacyId().equalsIgnoreCase(requesterPharmacyId)) {
            throw new IllegalStateException("Only the requester pharmacy can cancel this request.");
        }

        if (request.getStatus() != ExchangeRequest.RequestStatus.PENDING) {
            throw new IllegalStateException("Only PENDING requests can be cancelled.");
        }

        ExchangeListing listing = exchangeRepository.findListingById(request.getListingId());
        if (listing == null) {
            throw new IllegalStateException("Associated listing not found: " + request.getListingId());
        }

        synchronized (listing) {
            listing.releaseReservation(request.getRequestedQuantity());
            exchangeRepository.updateListing(listing);

            request.setStatus(ExchangeRequest.RequestStatus.CANCELLED);
            exchangeRepository.updateRequest(request);

            eventProducer.publishStockReleased(listing.getListingId(), requesterPharmacyId,
                    listing.getSellerPharmacyId(), request.getRequestedQuantity(), listing.getMedicineName());
        }
    }

    public List<ExchangeRequest> getRequestsForSeller(String sellerPharmacyId) {
        return exchangeRepository.findRequestsForSeller(sellerPharmacyId);
    }

    public List<ExchangeRequest> getRequestsByRequester(String requesterPharmacyId) {
        return exchangeRepository.findRequestsByRequester(requesterPharmacyId);
    }

    public List<ExchangeRequest> getAllRequests() {
        return exchangeRepository.findAllRequests();
    }

    // ==========================================
    // TRANSFER OPERATIONS
    // ==========================================

    public void updateTransferStatus(String transferId, StockTransfer.TransferStatus newStatus) {
        StockTransfer transfer = exchangeRepository.findTransferById(transferId);
        if (transfer == null) {
            throw new IllegalArgumentException("Transfer not found: " + transferId);
        }

        transfer.setStatus(newStatus);
        exchangeRepository.updateTransfer(transfer);

        if (newStatus == StockTransfer.TransferStatus.VERIFIED) {
            // Finalize permanent stock deduction
            ExchangeListing listing = exchangeRepository.findListingById(transfer.getListingId());
            if (listing != null) {
                synchronized (listing) {
                    listing.completeTransfer(transfer.getQuantity());
                    exchangeRepository.updateListing(listing);
                }
            }
            eventProducer.publishTransferCompleted(transfer);
        } else if (newStatus == StockTransfer.TransferStatus.DISPATCHED) {
            eventProducer.publishTransferDispatched(transfer);
        }
    }

    public List<StockTransfer> getTransfersForPharmacy(String pharmacyId) {
        return exchangeRepository.findTransfersByPharmacy(pharmacyId);
    }

    public List<StockTransfer> getAllTransfers() {
        return exchangeRepository.findAllTransfers();
    }

    // ==========================================
    // MULTITHREADING DEMONSTRATION SUITE
    // ==========================================

    /**
     * Spawns multiple concurrent threads attempting to reserve stock from the same listing.
     * Demonstrates that synchronized stock reservation prevents overselling and guarantees atomicity.
     */
    public String runConcurrentReservationTest(String listingId, int threadCount, int quantityPerThread) throws InterruptedException {
        ExchangeListing listing = exchangeRepository.findListingById(listingId);
        if (listing == null) {
            throw new IllegalArgumentException("Listing not found: " + listingId);
        }

        int initialAvailable = listing.getAvailableQuantity();
        System.out.printf("%n=== STARTING CONCURRENT RESERVATION TEST ===%n");
        System.out.printf("Listing: %s | Total Available: %d units | Threads: %d | Qty per Thread: %d%n",
                listingId, initialAvailable, threadCount, quantityPerThread);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<String> logs = Collections.synchronizedList(new ArrayList<>());

        for (int i = 1; i <= threadCount; i++) {
            final int pharmacyIndex = i;
            executor.submit(() -> {
                String pharmacyId = "PHARM-TEST-" + pharmacyIndex;
                String pharmacyName = "Test Pharmacy " + pharmacyIndex;
                try {
                    startLatch.await();
                    submitRequest(listingId, pharmacyId, pharmacyName, quantityPerThread,
                            listing.getUnitPrice(), "Concurrent automated test request");
                    successCount.incrementAndGet();
                    logs.add(String.format("Thread [%s]: Successfully reserved %d units.",
                            Thread.currentThread().getName(), quantityPerThread));
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    logs.add(String.format("Thread [%s]: Reservation failed (%s).",
                            Thread.currentThread().getName(), e.getMessage()));
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        finishLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        int finalAvailable = listing.getAvailableQuantity();
        int finalReserved = listing.getReservedQuantity();

        StringBuilder report = new StringBuilder();
        report.append(String.format("=== CONCURRENCY TEST SUMMARY ===%n"));
        report.append(String.format("Initial Available: %d units%n", initialAvailable));
        report.append(String.format("Threads Run: %d (Requested %d units each)%n", threadCount, quantityPerThread));
        report.append(String.format("Successful Reservations: %d%n", successCount.get()));
        report.append(String.format("Failed / Prevented Reservations: %d%n", failCount.get()));
        report.append(String.format("Final Reserved: %d units%n", finalReserved));
        report.append(String.format("Final Available: %d units%n", finalAvailable));
        report.append(String.format("Consistency Check: %s (No overselling detected)%n",
                (finalAvailable >= 0 && (finalReserved + finalAvailable == listing.getTotalQuantity())) ? "PASSED [OK]" : "FAILED [INCONSISTENT]"));

        System.out.println(report);
        return report.toString();
    }
}
