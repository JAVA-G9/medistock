package com.medistock.concurrency;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class ConcurrencyTest {

    @AfterEach
    public void tearDown() {
        ThreadManager.getInstance().shutdownAll();
    }

    @Test
    public void testExpiryMonitorIsDaemonThread() {
        ExpiryMonitorThread monitor = new ExpiryMonitorThread(500);

        // Multithreading syllabus verification:
        assertTrue(monitor.isDaemon(), "ExpiryMonitorThread MUST be configured as a daemon thread");
        assertTrue(monitor.getName().contains("Daemon"), "Thread name must indicate daemon nature");

        monitor.start();
        assertTrue(monitor.isAlive(), "Monitor thread should be alive after start()");

        monitor.stopMonitoring();
        try {
            monitor.join(2000);
        } catch (InterruptedException ignored) {
        }
        assertFalse(monitor.isAlive(), "Monitor thread should terminate cleanly after stop");
    }

    @Test
    public void testReportWorkerCustomPriorityAndExecution() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        ReportWorker worker = new ReportWorker("Test Audit", Thread.MAX_PRIORITY, latch::countDown);

        worker.start();
        assertEquals(Thread.MAX_PRIORITY, worker.getPriority());
        assertTrue(worker.getThread().getName().contains("MediStock-ReportWorker"));

        boolean finished = latch.await(4000, TimeUnit.MILLISECONDS);
        assertTrue(finished, "ReportWorker should complete background stages");
    }

    @Test
    public void testThreadLoggerCapturesTrace() {
        ThreadLogger.log("Testing logger execution trace");
        assertNotNull(ThreadLogger.getObservableLogs());
        // Pure headless check: ensures no exceptions thrown during logging
    }

    @Test
    public void testThreadManagerLifecycleObservation() throws InterruptedException {
        ThreadManager manager = ThreadManager.getInstance();
        manager.demonstrateAllSixStates();

        Thread.sleep(300); // allow threads to enter their states
        manager.refreshSnapshots();

        assertNotNull(manager.getObservableSnapshots());
        assertTrue(manager.getActiveThreadCount() > 0, "Active threads should be tracked by ThreadManager");

        manager.cleanupDemoThreads();
    }
}
