
package org.example.medistock.concurrency;

import javafx.application.Platform;
import org.example.medistock.model.ExpiryRecord;
import org.example.medistock.service.expiry.ExpiryService;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Background daemon thread for continuous monitoring of medicine batches
 * approaching expiration.
 *
 * Owned by Member 4 (Expiry, Notifications & Reports).
 *
 * Multithreading concepts:
 * Daemon thread, periodic polling, atomic variables, volatile flag,
 * interruption, and thread-safe listener management.
 */
public class ExpiryMonitorthread extends Thread {

    public interface ScanListener {
        void onScanCompleted(int cycle, List<ExpiryRecord> atRiskRecords);
    }

    private static ExpiryMonitorthread instance;

    private volatile boolean running = false;
    private volatile long intervalMillis;

    private final AtomicInteger scanCycle = new AtomicInteger(0);
    private final AtomicLong lastScanTime = new AtomicLong(0);
    private final AtomicInteger flaggedCount = new AtomicInteger(0);

    private final List<ScanListener> listeners =
            new CopyOnWriteArrayList<>();

    public ExpiryMonitorthread() {
        this(5000);
    }

    public ExpiryMonitorthread(long intervalMillis) {
        super("MediStock-ExpiryMonitor-Daemon");

        setIntervalMillis(intervalMillis);

        setDaemon(true);
        setPriority(Thread.NORM_PRIORITY - 1);
    }

    public static synchronized ExpiryMonitorthread getInstance() {
        if (instance == null || instance.getState() == State.TERMINATED) {
            instance = new ExpiryMonitorthread();
        }

        return instance;
    }

    public synchronized void startMonitoring() {

        if (isAlive()) {
            return;
        }

        if (getState() == State.TERMINATED) {
            System.err.println(
                    "[EXPIRY-DAEMON] Thread has already terminated. " +
                            "Use ExpiryMonitorthread.getInstance() to obtain a new instance."
            );
            return;
        }

        running = true;
        start();
    }

    public synchronized void stopMonitoring() {
        running = false;
        interrupt();
    }

    public void addListener(ScanListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(ScanListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void run() {

        System.out.printf(
                "[EXPIRY-DAEMON] [%s] Started. Daemon: %b, Priority: %d. Interval: %d ms.%n",
                getName(), isDaemon(), getPriority(), intervalMillis
        );

        while (running) {

            try {
                System.out.println(
                        "[EXPIRY-DAEMON] Scanning medicine batches..."
                );

                // Perform expiry scanning
                List<ExpiryRecord> records =
                        ExpiryService.getInstance()
                                .checkAndProcessExpiries("PHARM-001");

                // Avoid exposing a mutable list to listeners
                List<ExpiryRecord> urgentRecords =
                        records == null ? List.of() : List.copyOf(records);

                // Update monitoring statistics after successful scan
                int cycle = scanCycle.incrementAndGet();

                lastScanTime.set(System.currentTimeMillis());

                flaggedCount.set(urgentRecords.size());

                System.out.printf(
                        "[EXPIRY-DAEMON] Cycle #%d completed. Flagged %d batches.%n",
                        cycle, flaggedCount.get()
                );

                // Notify registered listeners
                for (ScanListener listener : listeners) {

                    try {
                        Platform.runLater(
                                () -> listener.onScanCompleted(cycle, urgentRecords)
                        );

                    } catch (IllegalStateException noToolkit) {

                        // Fallback for environments without JavaFX initialized
                        try {
                            listener.onScanCompleted(cycle, urgentRecords);
                        } catch (Exception e) {
                            System.err.println(
                                    "[EXPIRY-DAEMON] Listener error: " +
                                            e.getMessage()
                            );
                        }
                    }
                }

                // Thread enters TIMED_WAITING state
                Thread.sleep(intervalMillis);

            } catch (InterruptedException e) {

                System.out.printf(
                        "[EXPIRY-DAEMON] [%s] Interrupted. Stopping monitor.%n",
                        getName()
                );

                break;

            } catch (Exception e) {

                System.err.printf(
                        "[EXPIRY-DAEMON] Error during scan: %s%n",
                        e.getMessage()
                );

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException interrupted) {
                    break;
                }
            }
        }

        running = false;

        System.out.printf(
                "[EXPIRY-DAEMON] [%s] Stopped. Total successful scans: %d%n",
                getName(), scanCycle.get()
        );
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

        if (intervalMillis <= 0) {
            throw new IllegalArgumentException(
                    "Scan interval must be greater than zero."
            );
        }

        this.intervalMillis = intervalMillis;
    }
}
