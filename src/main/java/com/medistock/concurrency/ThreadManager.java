package com.medistock.concurrency;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Central manager for registering, observing, and demonstrating Java thread lifecycles,
 * daemon threads, and execution priorities.
 * Owner: Member 1.
 */
public class ThreadManager {

    private static final ThreadManager INSTANCE = new ThreadManager();

    private final Map<Thread, String> trackedThreads = new ConcurrentHashMap<>();
    private final ObservableList<ThreadInfoSnapshot> observableSnapshots = FXCollections.observableArrayList();

    private ExpiryMonitorThread daemonExpiryMonitor;
    private final Object demoMonitorLock = new Object();
    private final Object demoWaitingLock = new Object();
    private Thread demoBlockedHolder;
    private final List<Thread> stateDemoThreads = Collections.synchronizedList(new ArrayList<>());

    private ThreadManager() {
    }

    public static ThreadManager getInstance() {
        return INSTANCE;
    }

    /**
     * Register a thread to be tracked in the monitor.
     */
    public void registerThread(Thread thread, String purpose) {
        if (thread != null) {
            trackedThreads.put(thread, purpose);
            refreshSnapshots();
        }
    }

    /**
     * Unregister a thread.
     */
    public void unregisterThread(Thread thread) {
        if (thread != null) {
            trackedThreads.remove(thread);
            refreshSnapshots();
        }
    }

    /**
     * Start the background daemon ExpiryMonitorThread.
     */
    public synchronized void startDaemonExpiryMonitor() {
        if (daemonExpiryMonitor != null && daemonExpiryMonitor.isAlive()) {
            ThreadLogger.log("Daemon ExpiryMonitor is already running.");
            return;
        }

        daemonExpiryMonitor = new ExpiryMonitorThread(3000);
        registerThread(daemonExpiryMonitor, "Daemon Background Expiry Monitor");
        daemonExpiryMonitor.start();
        ThreadLogger.log("Registered and started daemon thread: " + daemonExpiryMonitor.getName());
        refreshSnapshots();
    }

    /**
     * Stop the background daemon ExpiryMonitorThread.
     */
    public synchronized void stopDaemonExpiryMonitor() {
        if (daemonExpiryMonitor != null) {
            daemonExpiryMonitor.stopMonitoring();
            ThreadLogger.log("Stopping daemon thread: " + daemonExpiryMonitor.getName());
            refreshSnapshots();
        }
    }

    public boolean isDaemonExpiryMonitorRunning() {
        return daemonExpiryMonitor != null && daemonExpiryMonitor.isAlive();
    }

    /**
     * Spawn a report worker with custom priority.
     */
    public void spawnReportWorker(String reportName, int priority) {
        ReportWorker worker = new ReportWorker(reportName, priority, this::refreshSnapshots);
        worker.start();
    }

    /**
     * Demonstrates all six Java Thread States:
     * 1. NEW: Thread instantiated but start() not called yet.
     * 2. RUNNABLE: Thread executing active computation.
     * 3. BLOCKED: Thread waiting to acquire an intrinsic monitor lock held by another thread.
     * 4. WAITING: Thread waiting indefinitely via monitor.wait().
     * 5. TIMED_WAITING: Thread sleeping via Thread.sleep(ms).
     * 6. TERMINATED: Thread that finished its execution.
     */
    public void demonstrateAllSixStates() {
        ThreadLogger.log("=== Initiating All 6 Java Thread States Demonstration ===");

        // Cleanup any previous demo threads
        cleanupDemoThreads();

        // 1. State: NEW
        Thread newThread = new Thread(() -> {
            // Intentionally not started immediately
        }, "Demo-State-NEW");
        registerThread(newThread, "Lifecycle Demo: NEW state (unstarted)");
        stateDemoThreads.add(newThread);

        // 2. State: RUNNABLE
        AtomicBoolean runnableKeepGoing = new AtomicBoolean(true);
        Thread runnableThread = new Thread(() -> {
            long count = 0;
            while (runnableKeepGoing.get()) {
                count++;
                if (count % 100_000_000 == 0) {
                    // Small yield so CPU isn't maxed out
                    Thread.yield();
                }
            }
        }, "Demo-State-RUNNABLE");
        registerThread(runnableThread, "Lifecycle Demo: RUNNABLE state (active execution)");
        runnableThread.start();
        stateDemoThreads.add(runnableThread);

        // 3. State: BLOCKED (Holder thread holds lock, target thread attempts to enter)
        demoBlockedHolder = new Thread(() -> {
            synchronized (demoMonitorLock) {
                try {
                    Thread.sleep(12_000); // Hold lock for 12 seconds
                } catch (InterruptedException ignored) {
                }
            }
        }, "Demo-LockHolder-For-BLOCKED");
        demoBlockedHolder.setDaemon(true);
        demoBlockedHolder.start();

        // Give holder thread a brief moment to acquire the lock
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {
        }

        Thread blockedThread = new Thread(() -> {
            synchronized (demoMonitorLock) {
                // Will block here waiting for demoBlockedHolder to release the lock
                ThreadLogger.log("Demo-State-BLOCKED thread finally acquired lock.");
            }
        }, "Demo-State-BLOCKED");
        registerThread(blockedThread, "Lifecycle Demo: BLOCKED state (waiting for intrinsic lock)");
        blockedThread.start();
        stateDemoThreads.add(blockedThread);

        // 4. State: WAITING (Calls wait() on an object)
        Thread waitingThread = new Thread(() -> {
            synchronized (demoWaitingLock) {
                try {
                    demoWaitingLock.wait(); // Indefinite wait until notify()
                } catch (InterruptedException ignored) {
                }
            }
        }, "Demo-State-WAITING");
        registerThread(waitingThread, "Lifecycle Demo: WAITING state (guarded wait())");
        waitingThread.start();
        stateDemoThreads.add(waitingThread);

        // 5. State: TIMED_WAITING (Calls Thread.sleep(ms))
        Thread timedWaitingThread = new Thread(() -> {
            try {
                Thread.sleep(15_000); // 15 seconds timed waiting
            } catch (InterruptedException ignored) {
            }
        }, "Demo-State-TIMED_WAITING");
        registerThread(timedWaitingThread, "Lifecycle Demo: TIMED_WAITING state (sleep)");
        timedWaitingThread.start();
        stateDemoThreads.add(timedWaitingThread);

        // 6. State: TERMINATED
        Thread terminatedThread = new Thread(() -> {
            // Finishes immediately
        }, "Demo-State-TERMINATED");
        registerThread(terminatedThread, "Lifecycle Demo: TERMINATED state (completed)");
        terminatedThread.start();
        try {
            terminatedThread.join(); // Wait for it to finish immediately
        } catch (InterruptedException ignored) {
        }
        stateDemoThreads.add(terminatedThread);

        // Schedule stopping the busy RUNNABLE and releasing WAITING after 10 seconds
        Thread cleanupTimer = new Thread(() -> {
            try {
                Thread.sleep(10_000);
                runnableKeepGoing.set(false);
                synchronized (demoWaitingLock) {
                    demoWaitingLock.notifyAll();
                }
                ThreadLogger.log("Demo threads auto-released after demonstration cycle.");
                refreshSnapshots();
            } catch (InterruptedException ignored) {
            }
        }, "Demo-AutoCleanup");
        cleanupTimer.setDaemon(true);
        cleanupTimer.start();

        ThreadLogger.log("All 6 Thread States successfully staged. Refreshing live thread inspector.");
        refreshSnapshots();
    }

    /**
     * Clean up active demo threads if any are still running.
     */
    public void cleanupDemoThreads() {
        synchronized (demoWaitingLock) {
            demoWaitingLock.notifyAll();
        }
        if (demoBlockedHolder != null && demoBlockedHolder.isAlive()) {
            demoBlockedHolder.interrupt();
        }
        for (Thread t : stateDemoThreads) {
            if (t.isAlive()) {
                t.interrupt();
            }
        }
        stateDemoThreads.clear();
    }

    /**
     * Refreshes the ObservableList of snapshots from all tracked threads.
     */
    public void refreshSnapshots() {
        List<ThreadInfoSnapshot> snapshots = new ArrayList<>();

        // Add current JavaFX application thread for reference
        Thread fxThread = findFxThread();
        if (fxThread != null && !trackedThreads.containsKey(fxThread)) {
            snapshots.add(ThreadInfoSnapshot.of(fxThread, "JavaFX Application UI Thread"));
        }

        Iterator<Map.Entry<Thread, String>> iterator = trackedThreads.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Thread, String> entry = iterator.next();
            Thread t = entry.getKey();
            String purpose = entry.getValue();
            snapshots.add(ThreadInfoSnapshot.of(t, purpose));
        }

        try {
            Platform.runLater(() -> observableSnapshots.setAll(snapshots));
        } catch (Throwable ignored) {
            observableSnapshots.setAll(snapshots);
        }
    }

    private Thread findFxThread() {
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (t.getName().toLowerCase().contains("javafx application thread")) {
                return t;
            }
        }
        return null;
    }

    public ObservableList<ThreadInfoSnapshot> getObservableSnapshots() {
        return observableSnapshots;
    }

    public int getActiveThreadCount() {
        int count = 0;
        for (Thread t : trackedThreads.keySet()) {
            if (t.isAlive()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Gracefully shutdown all background tasks and threads.
     */
    public void shutdownAll() {
        ThreadLogger.log("Shutting down all MediStock background threads...");
        stopDaemonExpiryMonitor();
        cleanupDemoThreads();
        for (Thread t : trackedThreads.keySet()) {
            if (t.isAlive()) {
                t.interrupt();
            }
        }
        trackedThreads.clear();
        ThreadLogger.log("ThreadManager shutdown complete.");
    }
}
