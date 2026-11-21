package com.medistock.concurrency;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Worker thread for generating comprehensive inventory audit and exchange reports in the background.
 * Owner: Member 1.
 * Demonstrates: Thread priority manipulation, thread naming, non-blocking UI operations, and state progression.
 */
public class ReportWorker implements Runnable {

    private static final AtomicInteger COUNTER = new AtomicInteger(1);

    private final int reportId;
    private final String reportType;
    private final int priority;
    private final Runnable onCompletion;
    private Thread thread;

    public ReportWorker(String reportType, int priority, Runnable onCompletion) {
        this.reportId = COUNTER.getAndIncrement();
        this.reportType = reportType;
        this.priority = Math.clamp(priority, Thread.MIN_PRIORITY, Thread.MAX_PRIORITY);
        this.onCompletion = onCompletion;
    }

    public void start() {
        String threadName = String.format("MediStock-ReportWorker-%d-[Pri=%d]", reportId, priority);
        thread = new Thread(this, threadName);
        thread.setPriority(priority);
        thread.start();
        ThreadManager.getInstance().registerThread(thread, "Report Generator: " + reportType);
    }

    @Override
    public void run() {
        ThreadLogger.log(String.format("Starting background report '%s' with priority %d...", reportType, priority));

        try {
            // Stage 1: Collecting inventory data
            ThreadLogger.log("Stage 1/4: Aggregating stock batches and warehouse records...");
            Thread.sleep(600);

            // Stage 2: Cross-referencing marketplace requests
            ThreadLogger.log("Stage 2/4: Cross-referencing exchange requests and transfer status...");
            Thread.sleep(600);

            // Stage 3: Risk assessment on near-expiry batches
            ThreadLogger.log("Stage 3/4: Calculating financial risk on near-expiry batches...");
            Thread.sleep(600);

            // Stage 4: Formatting and output
            ThreadLogger.log(String.format("Stage 4/4: Finalizing '%s'. Report generated successfully.", reportType));
            Thread.sleep(300);

        } catch (InterruptedException e) {
            ThreadLogger.log("Report generation was interrupted.");
            Thread.currentThread().interrupt();
        } finally {
            ThreadLogger.log(String.format("Report worker #%d finished execution. Thread entering TERMINATED state.", reportId));
            if (onCompletion != null) {
                onCompletion.run();
            }
        }
    }

    public Thread getThread() {
        return thread;
    }

    public int getPriority() {
        return priority;
    }

    public String getReportType() {
        return reportType;
    }
}
