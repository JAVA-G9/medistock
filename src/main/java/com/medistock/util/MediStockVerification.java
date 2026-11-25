package com.medistock.util;

import com.medistock.concurrency.ExpiryMonitorThread;
import com.medistock.concurrency.ReportWorker;
import com.medistock.concurrency.ThreadLogger;
import com.medistock.concurrency.ThreadManager;
import com.medistock.model.Pharmacy;
import com.medistock.model.User;
import com.medistock.service.PharmacyService;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Self-contained verification runner for Member 1 deliverables.
 * Executes automated validations for repository, service, and concurrency mechanics.
 */
public class MediStockVerification {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("  MediStock Member 1 Verification Suite");
        System.out.println("==================================================");

        int passed = 0;
        int failed = 0;

        // 1. Repository & Service Verification
        try {
            System.out.print("[1/5] Testing Pharmacy Service & Database Authentication... ");
            PharmacyService service = PharmacyService.getInstance();
            User user = service.login("admin", "admin123");
            if (user == null || !user.getUsername().equals("admin")) {
                throw new AssertionError("User login failed");
            }
            if (!SessionContext.isAuthenticated()) {
                throw new AssertionError("SessionContext not set on login");
            }
            if (!SessionContext.getCurrentPharmacy().getId().equals("PHARM-001")) {
                throw new AssertionError("Incorrect pharmacy attached to user");
            }
            System.out.println("PASSED (Logged in as: " + user.getFullName() + " | DB Repo: " + service.getRepository().getClass().getSimpleName() + ")");
            passed++;
        } catch (Exception e) {
            System.out.println("FAILED: " + e.getMessage());
            failed++;
        }

        // 2. Pharmacy Registration Verification
        try {
            System.out.print("[2/5] Testing Pharmacy & User Registration... ");
            PharmacyService service = PharmacyService.getInstance();
            long uniqueSuffix = System.currentTimeMillis() % 100000;
            String license = "DL-2024-HF" + uniqueSuffix;
            String username = "healthfirst_" + uniqueSuffix;
            Pharmacy testPharma = new Pharmacy(
                    null,
                    "HealthFirst Community Pharmacy",
                    license,
                    "88 Elm Street",
                    "Metro City",
                    "+1 555-432-1098",
                    "info@healthfirst.com"
            );
            User testUser = new User(
                    null, null, username, "password123",
                    "Dr. Elena Rostova", "elena@healthfirst.com", "Chief Pharmacist"
            );
            User reg = service.register(testPharma, testUser);
            if (reg == null || !reg.getUsername().equals(username)) {
                throw new AssertionError("Registration failed");
            }
            System.out.println("PASSED (Registered: " + testPharma.getName() + " [" + testPharma.getLicenseNumber() + "])");
            passed++;
        } catch (Exception e) {
            System.out.println("FAILED: " + e.getMessage());
            failed++;
        }

        // 3. ExpiryMonitorThread Daemon Verification
        try {
            System.out.print("[3/5] Testing ExpiryMonitor Daemon Thread... ");
            ExpiryMonitorThread monitor = new ExpiryMonitorThread(400);
            if (!monitor.isDaemon()) {
                throw new AssertionError("ExpiryMonitorThread must have daemon=true");
            }
            if (!monitor.getName().contains("Daemon")) {
                throw new AssertionError("ExpiryMonitorThread name must include Daemon descriptor");
            }
            monitor.start();
            Thread.sleep(600);
            monitor.stopMonitoring();
            monitor.join(2000);
            if (monitor.isAlive()) {
                throw new AssertionError("ExpiryMonitorThread did not terminate cleanly");
            }
            System.out.println("PASSED (Daemon=true, Name=" + monitor.getName() + ")");
            passed++;
        } catch (Exception e) {
            System.out.println("FAILED: " + e.getMessage());
            failed++;
        }

        // 4. ReportWorker Priority & Execution Tracing Verification
        try {
            System.out.print("[4/5] Testing ReportWorker Priorities & Tracing... ");
            CountDownLatch latch = new CountDownLatch(1);
            ReportWorker worker = new ReportWorker("Stock Audit", Thread.MAX_PRIORITY, latch::countDown);
            if (worker.getPriority() != Thread.MAX_PRIORITY) {
                throw new AssertionError("Priority not correctly set to MAX_PRIORITY (10)");
            }
            worker.start();
            boolean completed = latch.await(15, TimeUnit.SECONDS);
            if (!completed) {
                throw new AssertionError("ReportWorker timed out");
            }
            System.out.println("PASSED (Priority=" + worker.getPriority() + ", stages logged)");
            passed++;
        } catch (Exception e) {
            System.out.println("FAILED: " + e.getMessage());
            failed++;
        }

        // 5. ThreadManager 6-State Lifecycle Demonstration Verification
        try {
            System.out.print("[5/5] Testing 6 Thread States Lifecycle Demonstration... ");
            ThreadManager manager = ThreadManager.getInstance();
            manager.demonstrateAllSixStates();
            Thread.sleep(300); // Allow thread states to settle
            manager.refreshSnapshots();
            int active = manager.getActiveThreadCount();
            if (active <= 0) {
                throw new AssertionError("No active threads tracked during demonstration");
            }
            manager.cleanupDemoThreads();
            manager.shutdownAll();
            System.out.println("PASSED (All 6 states staged, " + active + " active threads tracked)");
            passed++;
        } catch (Exception e) {
            System.out.println("FAILED: " + e.getMessage());
            failed++;
        }

        System.out.println("==================================================");
        System.out.printf("  Summary: %d / %d tests passed (Failed: %d)%n", passed, (passed + failed), failed);
        System.out.println("==================================================");

        System.exit(failed > 0 ? 1 : 0);
    }
}
