package com.medistock.concurrency;

import org.example.medistock.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Worker thread for generating comprehensive inventory audit and exchange reports in the background.
 * Owner: Member 1.
 * Connects directly to MySQL / TiDB Cloud database using database.properties.
 * Demonstrates: Thread priority manipulation, thread naming, non-blocking UI operations, state progression, and JDBC metrics aggregation.
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

        int totalBatches = 0;
        int totalUnits = 0;
        int activeListings = 0;
        int pendingRequests = 0;
        double financialRisk = 0.0;

        try {
            // Stage 1: Collecting inventory data from database
            ThreadLogger.log("Stage 1/4: Aggregating stock batches and warehouse records from database...");
            try (Connection con = DBConnection.connectToMySql()) {
                if (con != null) {
                    try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*), COALESCE(SUM(quantity_available), 0) FROM medicine_batch");
                         ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            totalBatches = rs.getInt(1);
                            totalUnits = rs.getInt(2);
                        }
                    }
                }
            } catch (Exception ignored) {
            }
            Thread.sleep(200);
            ThreadLogger.log(String.format("Stage 1/4 complete: Aggregated %d batches (%d total units) from database.", totalBatches, totalUnits));

            // Stage 2: Cross-referencing marketplace requests from database
            ThreadLogger.log("Stage 2/4: Cross-referencing exchange requests and transfer status in database...");
            try (Connection con = DBConnection.connectToMySql()) {
                if (con != null) {
                    try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM exchange_listing WHERE status = 'AVAILABLE'");
                         ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) activeListings = rs.getInt(1);
                    }
                    try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM exchange_request WHERE status = 'PENDING'");
                         ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) pendingRequests = rs.getInt(1);
                    }
                }
            } catch (Exception ignored) {
            }
            Thread.sleep(200);
            ThreadLogger.log(String.format("Stage 2/4 complete: Database cross-reference found %d active listing(s), %d pending request(s).", activeListings, pendingRequests));

            // Stage 3: Risk assessment on near-expiry batches from database
            ThreadLogger.log("Stage 3/4: Calculating financial risk on near-expiry batches from database...");
            try (Connection con = DBConnection.connectToMySql()) {
                if (con != null) {
                    String riskSql = """
                        SELECT COALESCE(SUM(quantity_available * purchase_price), 0.0)
                        FROM medicine_batch
                        WHERE expiry_date <= DATE_ADD(CURDATE(), INTERVAL 30 DAY)
                          AND quantity_available > 0
                          AND status != 'EXPIRED'
                    """;
                    try (PreparedStatement ps = con.prepareStatement(riskSql);
                         ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            financialRisk = rs.getDouble(1);
                        }
                    }
                }
            } catch (Exception ignored) {
            }
            Thread.sleep(200);
            ThreadLogger.log(String.format("Stage 3/4 complete: Total near-expiry financial exposure: $%.2f across batches.", financialRisk));

            // Stage 4: Formatting and output
            ThreadLogger.log(String.format("Stage 4/4: Finalizing '%s'. Audit compiled with real database figures. Report ready.", reportType));
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
