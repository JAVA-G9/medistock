package org.example.medistock;

import org.example.medistock.concurrency.CustomEventQueue;
import org.example.medistock.concurrency.EventProducer;
import org.example.medistock.concurrency.ExchangeWorker;
import org.example.medistock.model.AppEvent;
import org.example.medistock.model.ExchangeListing;
import org.example.medistock.model.ExchangeRequest;
import org.example.medistock.model.StockTransfer;
import org.example.medistock.repository.exchange.InMemoryExchangeRepository;
import org.example.medistock.service.exchange.ExchangeService;

import java.time.LocalDate;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Standalone verification suite for Member 3 (Stock Exchange & Requests).
 * Tests all backend features, concurrent reservation, guarded event queue,
 * and worker thread lifecycle without requiring external test runners.
 */
public class ExchangeBackendVerification {

    private static int passedTests = 0;
    private static int totalTests = 0;

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println(" PHARMAEXCHANGE / MEDISTOCK - MEMBER 3 VERIFICATION SUITE");
        System.out.println(" Domain: Stock Exchange, Requests & Concurrency Coordination");
        System.out.println("===============================================================\n");

        runTest("1. Create Surplus Listing & Querying", ExchangeBackendVerification::testCreateListing);
        runTest("2. Atomic Stock Reservation on Request", ExchangeBackendVerification::testStockReservation);
        runTest("3. Multithreading: 10 Concurrent Threads Prevent Overselling", ExchangeBackendVerification::testConcurrentReservation);
        runTest("4. Approve Request & Generate Stock Transfer", ExchangeBackendVerification::testApproveRequest);
        runTest("5. Reject Request & Atomic Stock Release", ExchangeBackendVerification::testRejectRequest);
        runTest("6. CustomEventQueue: Guarded wait()/notifyAll() Coordination", ExchangeBackendVerification::testCustomEventQueueCoordination);
        runTest("7. ExchangeWorker: Named Daemon Thread Lifecycle Tracing", ExchangeBackendVerification::testWorkerLifecycle);

        System.out.println("\n===============================================================");
        System.out.printf(" RESULTS: %d / %d TESTS PASSED SUCCESSFULLY!%n", passedTests, totalTests);
        System.out.println("===============================================================");

        if (passedTests != totalTests) {
            System.exit(1);
        } else {
            System.exit(0);
        }
    }

    private static void runTest(String testName, RunnableWithException test) {
        totalTests++;
        System.out.printf("[RUNNING] %s ... ", testName);
        try {
            test.run();
            passedTests++;
            System.out.println("PASSED [OK]");
        } catch (Throwable t) {
            System.out.println("FAILED [FAIL]");
            System.err.println("  Error: " + t.getMessage());
            t.printStackTrace(System.err);
        }
    }

    // 1. Create Listing
    private static void testCreateListing() {
        InMemoryExchangeRepository repo = new InMemoryExchangeRepository();
        CustomEventQueue queue = new CustomEventQueue(10);
        EventProducer producer = new EventProducer(queue);
        ExchangeService service = new ExchangeService(repo, producer);

        ExchangeListing listing = service.createListing(
                "PHARM-001", "Apollo Pharmacy", "Amoxicillin 500mg", "AMX-2024",
                LocalDate.now().plusMonths(3), 100, 85.0, "Near expiry stock"
        );

        assertNotNull(listing, "Listing should not be null");
        assertEquals(100, listing.getTotalQuantity(), "Total quantity must match");
        assertEquals(100, listing.getAvailableQuantity(), "Available quantity must match");
        assertEquals(0, listing.getReservedQuantity(), "Reserved quantity should start at 0");
        assertEquals(ExchangeListing.ListingStatus.AVAILABLE, listing.getStatus(), "Status must be AVAILABLE");
    }

    // 2. Atomic Reservation
    private static void testStockReservation() {
        InMemoryExchangeRepository repo = new InMemoryExchangeRepository();
        CustomEventQueue queue = new CustomEventQueue(10);
        EventProducer producer = new EventProducer(queue);
        ExchangeService service = new ExchangeService(repo, producer);

        ExchangeListing listing = service.createListing(
                "PHARM-001", "Apollo Pharmacy", "Paracetamol 650mg", "PCM-2024",
                LocalDate.now().plusMonths(4), 60, 25.0, "Surplus"
        );

        ExchangeRequest req = service.submitRequest(
                listing.getListingId(), "PHARM-002", "MedPlus Chemist",
                25, 25.0, "Request for 25 units"
        );

        assertNotNull(req, "Request should be created");
        assertEquals(ExchangeRequest.RequestStatus.PENDING, req.getStatus(), "Status must be PENDING");
        assertEquals(35, listing.getAvailableQuantity(), "Available units should be 60 - 25 = 35");
        assertEquals(25, listing.getReservedQuantity(), "Reserved units should be 25");
    }

    // 3. Multithreading Concurrent Reservation
    private static void testConcurrentReservation() throws Exception {
        InMemoryExchangeRepository repo = new InMemoryExchangeRepository();
        CustomEventQueue queue = new CustomEventQueue(50);
        EventProducer producer = new EventProducer(queue);
        ExchangeService service = new ExchangeService(repo, producer);

        ExchangeListing listing = service.createListing(
                "PHARM-001", "Apollo Pharmacy", "Azithromycin 500mg", "AZI-CONCUR",
                LocalDate.now().plusMonths(3), 50, 100.0, "Stress Test Batch"
        );

        int threadCount = 10;
        int qtyPerThread = 15;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger failures = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final String buyerId = "PHARM-BUYER-" + (i + 1);
            executor.submit(() -> {
                try {
                    startLatch.await();
                    service.submitRequest(
                            listing.getListingId(), buyerId, "Buyer " + buyerId,
                            qtyPerThread, 100.0, "Concurrent automated order"
                    );
                    successes.incrementAndGet();
                } catch (Exception e) {
                    failures.incrementAndGet();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean finished = finishLatch.await(5, TimeUnit.SECONDS);
        assertTrue(finished, "All threads should complete within 5 seconds");
        executor.shutdown();

        // 50 total units / 15 per request = exactly 3 can succeed (3 * 15 = 45 units reserved)
        // 7 threads must be safely rejected because 5 units remaining < 15
        assertEquals(3, successes.get(), "Exactly 3 requests should succeed");
        assertEquals(7, failures.get(), "Exactly 7 requests should be rejected");
        assertEquals(45, listing.getReservedQuantity(), "Reserved quantity must be 45");
        assertEquals(5, listing.getAvailableQuantity(), "Available quantity must be 5");
        assertTrue(listing.getAvailableQuantity() >= 0, "Stock cannot be oversold");
    }

    // 4. Approve Request & Transfer Creation
    private static void testApproveRequest() {
        InMemoryExchangeRepository repo = new InMemoryExchangeRepository();
        CustomEventQueue queue = new CustomEventQueue(10);
        EventProducer producer = new EventProducer(queue);
        ExchangeService service = new ExchangeService(repo, producer);

        ExchangeListing listing = service.createListing(
                "PHARM-001", "Apollo Pharmacy", "Cetirizine 10mg", "CET-001",
                LocalDate.now().plusMonths(5), 100, 15.0, "Ready"
        );

        ExchangeRequest req = service.submitRequest(
                listing.getListingId(), "PHARM-002", "MedPlus Chemist",
                30, 15.0, "Need urgent cetirizine"
        );

        StockTransfer transfer = service.approveRequest(req.getRequestId(), "PHARM-001");
        assertNotNull(transfer, "Transfer should be created");
        assertEquals(StockTransfer.TransferStatus.INITIATED, transfer.getStatus(), "Transfer status must be INITIATED");
        assertEquals(30, transfer.getQuantity(), "Transfer quantity must match");
        assertEquals(ExchangeRequest.RequestStatus.APPROVED, req.getStatus(), "Request must be APPROVED");
    }

    // 5. Reject Request & Release Stock
    private static void testRejectRequest() {
        InMemoryExchangeRepository repo = new InMemoryExchangeRepository();
        CustomEventQueue queue = new CustomEventQueue(10);
        EventProducer producer = new EventProducer(queue);
        ExchangeService service = new ExchangeService(repo, producer);

        ExchangeListing listing = service.createListing(
                "PHARM-001", "Apollo Pharmacy", "Omeprazole 20mg", "OME-001",
                LocalDate.now().plusMonths(2), 80, 20.0, "Surplus"
        );

        ExchangeRequest req = service.submitRequest(
                listing.getListingId(), "PHARM-002", "MedPlus Chemist",
                35, 20.0, "Request"
        );

        assertEquals(45, listing.getAvailableQuantity(), "Available units should be 45");
        assertEquals(35, listing.getReservedQuantity(), "Reserved units should be 35");

        // Reject request
        service.rejectRequest(req.getRequestId(), "PHARM-001", "Payment dispute");

        assertEquals(ExchangeRequest.RequestStatus.REJECTED, req.getStatus(), "Request must be REJECTED");
        assertEquals(80, listing.getAvailableQuantity(), "Available units should be restored to 80");
        assertEquals(0, listing.getReservedQuantity(), "Reserved units should be reset to 0");
    }

    // 6. CustomEventQueue Coordination
    private static void testCustomEventQueueCoordination() throws Exception {
        CustomEventQueue queue = new CustomEventQueue(3); // Small capacity of 3
        AtomicInteger consumed = new AtomicInteger(0);

        Thread consumer = new Thread(() -> {
            try {
                while (!queue.isShutdown()) {
                    AppEvent evt = queue.take();
                    if (evt != null) {
                        consumed.incrementAndGet();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Test-Consumer-Worker");

        consumer.start();

        // Produce 6 events (exceeds queue capacity of 3, tests producer waiting for consumer)
        for (int i = 1; i <= 6; i++) {
            AppEvent evt = new AppEvent("EVT-" + i, AppEvent.EventType.LISTING_CREATED,
                    "PHARM-A", "PHARM-B", "REF-" + i, "Event payload " + i);
            queue.put(evt);
        }

        Thread.sleep(150);
        queue.shutdown();
        consumer.join(1000);

        assertEquals(6, consumed.get(), "All 6 events should be consumed through guarded wait/notify");
    }

    // 7. ExchangeWorker Lifecycle
    private static void testWorkerLifecycle() throws Exception {
        InMemoryExchangeRepository repo = new InMemoryExchangeRepository();
        CustomEventQueue queue = new CustomEventQueue(10);
        EventProducer producer = new EventProducer(queue);
        ExchangeService service = new ExchangeService(repo, producer);

        ExchangeWorker worker = new ExchangeWorker(service, 100);
        assertEquals(Thread.State.NEW, worker.getThreadState(), "Worker state before start should be NEW");

        worker.start();
        assertTrue(worker.isRunning(), "Worker should be running");

        Thread.sleep(150);
        Thread.State activeState = worker.getThreadState();
        assertTrue(activeState == Thread.State.RUNNABLE || activeState == Thread.State.TIMED_WAITING,
                "Worker state should be RUNNABLE or TIMED_WAITING, got: " + activeState);

        worker.stop();
        Thread.sleep(100);
        assertFalse(worker.isRunning(), "Worker should be stopped");
    }

    // Assertion helpers
    private static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError("Assertion failed: " + message);
    }

    private static void assertFalse(boolean condition, String message) {
        if (condition) throw new AssertionError("Assertion failed: " + message);
    }

    private static void assertNotNull(Object obj, String message) {
        if (obj == null) throw new AssertionError("Assertion failed: " + message);
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(String.format("%s [Expected: %s, Actual: %s]", message, expected, actual));
        }
    }

    @FunctionalInterface
    interface RunnableWithException {
        void run() throws Throwable;
    }
}
