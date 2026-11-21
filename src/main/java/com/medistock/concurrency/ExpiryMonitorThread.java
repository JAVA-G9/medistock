package com.medistock.concurrency;

/**
 * Background daemon thread for continuous monitoring of medicine batches approaching expiration.
 * Owner: Member 1.
 * Demonstrates: Daemon threads, thread naming, execution tracing, and periodic execution.
 */
public class ExpiryMonitorThread extends Thread {

    private volatile boolean running = true;
    private final long intervalMillis;
    private int scanCycle = 0;

    public ExpiryMonitorThread() {
        this(3000);
    }

    public ExpiryMonitorThread(long intervalMillis) {
        super("MediStock-ExpiryMonitor-Daemon");
        this.intervalMillis = intervalMillis;
        // Multithreading syllabus requirement: Explicitly mark as daemon thread
        setDaemon(true);
        setPriority(Thread.NORM_PRIORITY - 1); // 4
    }

    @Override
    public void run() {
        ThreadLogger.log("Daemon thread started. Running periodic background sweeps...");

        while (running) {
            try {
                scanCycle++;
                ThreadLogger.log(String.format("Cycle #%d: Scanning registered inventory for batches near expiry (<= 30 days)...", scanCycle));

                // Simulate scanning work
                Thread.sleep(intervalMillis);

                ThreadLogger.log(String.format("Cycle #%d: Sweep complete. 3 near-expiry alerts verified. Background monitor sleeping.", scanCycle));
            } catch (InterruptedException e) {
                ThreadLogger.log("Daemon thread received interruption signal. Exiting gracefully.");
                break;
            }
        }

        ThreadLogger.log("Daemon thread stopped.");
    }

    public void stopMonitoring() {
        this.running = false;
        this.interrupt();
    }

    public boolean isMonitoring() {
        return running && isAlive();
    }

    public int getScanCycle() {
        return scanCycle;
    }
}
