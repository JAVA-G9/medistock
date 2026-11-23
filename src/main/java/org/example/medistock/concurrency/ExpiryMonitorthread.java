package org.example.medistock.concurrency;

import javafx.application.Platform;
import org.example.medistock.model.ExpiryRecord;
import org.example.medistock.service.expiry.ExpiryService;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Background daemon thread for continuous monitoring of medicine batches approaching expiration.
 * Owned by Member 4 (Expiry, Notifications & Reports).
 * Multithreading syllabus role: Daemon thread, periodic polling, state tracing (RUNNABLE / TIMED_WAITING),
 * safe shutdown using volatile running flag and interrupt().
 */
public class ExpiryMonitorthread extends Thread {

    public interface ScanListener {
        void onScanCompleted(int cycle, List<ExpiryRecord> atRiskRecords);
    }

    private static ExpiryMonitorthread instance;
    private volatile boolean running = false;
    private long intervalMillis = 5000; // 5 seconds default
    private final AtomicInteger scanCycle = new AtomicInteger(0);
    private final AtomicLong lastScanTime = new AtomicLong(0);
    private final AtomicInteger flaggedCount = new AtomicInteger(0);

    private final List<ScanListener> listeners = new CopyOnWriteArrayList<>();

    public ExpiryMonitorthread() {
        this(5000);
        this(10000);
    }

    public ExpiryMonitorthread(long intervalMillis) {
        super("MediStock-ExpiryMonitor-Daemon");
        this.intervalMillis = intervalMillis;
        setDaemon(true); // Daemon thread: does not prevent JVM termination
        setPriority(Thread.NORM_PRIORITY - 1); // Priority 4
    }

    public static synchronized ExpiryMonitorthread getInstance() {
        if (instance == null || !instance.isAlive()) {
            instance = new ExpiryMonitorthread();
        }
        return instance;
    }

    public synchronized void startMonitoring() {
        if (running && isAlive()) {
            return;
        }
        running = true;
        if (!isAlive()) {
            try {
                start();
            } catch (IllegalThreadStateException e) {
                // If thread already finished or started, create fresh instance
                instance = new ExpiryMonitorthread(this.intervalMillis);
                instance.listeners.addAll(this.listeners);
                instance.running = true;
                instance.start();
            }
        }
    }

    public synchronized void stopMonitoring() {
        this.running = false;
        this.interrupt();
    }

    public void addListener(ScanListener listener) {
        if (listener != null && !listeners.contains(listener)) {
        if (listener != null) {
            listeners.removeIf(l -> l.getClass().equals(listener.getClass()));
            listeners.add(listener);
        }
    }

    public void removeListener(ScanListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void run() {
        System.out.printf("[EXPIRY-DAEMON] [%s] Daemon thread started (Daemon: %b, Priority: %d). Periodic sweeps every %d ms...%n",
                getName(), isDaemon(), getPriority(), intervalMillis);

        while (running) {
            try {
                lastScanTime.set(System.currentTimeMillis());

                // Run sweep through ExpiryService first
                List<ExpiryRecord> urgentRecords = ExpiryService.getInstance().checkAndProcessExpiries("PHARM-001");

                // Increment ONLY after successful sweep execution
                int cycle = scanCycle.incrementAndGet();
                flaggedCount.set(urgentRecords != null ? urgentRecords.size() : 0);

                System.out.printf("[EXPIRY-DAEMON] Cycle #%d: Sweep successfully completed. Flagged %d at-risk/expired batches.%n",
                int cycle = scanCycle.incrementAndGet();
                lastScanTime.set(System.currentTimeMillis());

                System.out.printf("[EXPIRY-DAEMON] Cycle #%d: Scanning medicine batches for expiration risks...%n", cycle);

                // Run sweep through ExpiryService
                List<ExpiryRecord> urgentRecords = ExpiryService.getInstance().checkAndProcessExpiries("PHARM-001");
                flaggedCount.set(urgentRecords != null ? urgentRecords.size() : 0);

                System.out.printf("[EXPIRY-DAEMON] Cycle #%d: Sweep complete. Flagged %d at-risk/expired batches.%n",
                        cycle, flaggedCount.get());

                // Notify UI listeners safely
                for (ScanListener listener : listeners) {
                    try {
                        try {
                            Platform.runLater(() -> listener.onScanCompleted(cycle, urgentRecords));
                        } catch (IllegalStateException noToolkit) {
                            // Non-UI testing environment
                            listener.onScanCompleted(cycle, urgentRecords);
                        }
                    } catch (Exception e) {
                        System.err.println("[EXPIRY-DAEMON] Error notifying listener: " + e.getMessage());
                    }
                }

                // Enters TIMED_WAITING state
                Thread.sleep(intervalMillis);

            } catch (InterruptedException e) {
                System.out.printf("[EXPIRY-DAEMON] [%s] Received interruption signal. Stopping monitor loop.%n", getName());
                break;
            } catch (Exception e) {
                System.err.printf("[EXPIRY-DAEMON] Error during scan cycle: %s%n", e.getMessage());
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    break;
                }
            }
        }

        System.out.printf("[EXPIRY-DAEMON] [%s] Daemon thread stopped. Final cycle count: %d%n",
                getName(), scanCycle.get());
    }

    public int getScanCycle() {
        return scanCycle.get();
    }

    public long getLastScanTime() {
        return lastScanTime.get();
    }

    public int getFlaggedCount() {
        return flaggedCount.get();
    }

    public boolean isMonitoring() {
        return running && isAlive();
    }

    public long getIntervalMillis() {
        return intervalMillis;
    }

    public void setIntervalMillis(long intervalMillis) {
        this.intervalMillis = intervalMillis;
    }
}
