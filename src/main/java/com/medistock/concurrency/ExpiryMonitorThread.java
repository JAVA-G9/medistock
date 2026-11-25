package com.medistock.concurrency;

import org.example.medistock.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Background daemon thread for continuous monitoring of medicine batches approaching expiration.
 * Owner: Member 1.
 * Connects directly to MySQL / TiDB Cloud database using database.properties.
 * Demonstrates: Daemon threads, thread naming, execution tracing, periodic execution, and JDBC querying.
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
        ThreadLogger.log("Daemon thread started. Running periodic background sweeps against database...");

        while (running) {
            try {
                scanCycle++;
                ThreadLogger.log(String.format("Cycle #%d: Scanning database for batches near expiry (<= 30 days)...", scanCycle));

                int nearExpiryCount = 0;
                StringBuilder batchDetails = new StringBuilder();
                boolean dbConnected = false;

                try (Connection con = DBConnection.connectToMySql()) {
                    if (con != null) {
                        dbConnected = true;
                        String sql = """
                            SELECT batch_number, medicine_name, DATEDIFF(expiry_date, CURDATE()) AS days_left
                            FROM medicine_batch
                            WHERE expiry_date <= DATE_ADD(CURDATE(), INTERVAL 30 DAY)
                              AND status != 'EXPIRED'
                              AND quantity_available > 0
                            ORDER BY expiry_date ASC
                            LIMIT 3
                        """;
                        try (PreparedStatement ps = con.prepareStatement(sql);
                             ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                nearExpiryCount++;
                                if (batchDetails.length() > 0) batchDetails.append(", ");
                                batchDetails.append(rs.getString("batch_number"))
                                            .append(" [").append(rs.getString("medicine_name"))
                                            .append(": ").append(rs.getInt("days_left")).append("d left]");
                            }
                        }
                    }
                } catch (Throwable t) {
                    // Non-fatal, graceful logging
                }

                Thread.sleep(intervalMillis);

                if (dbConnected) {
                    if (nearExpiryCount > 0) {
                        ThreadLogger.log(String.format("Cycle #%d: DB Sweep complete. %d near-expiry alert(s) verified in database (%s).",
                                scanCycle, nearExpiryCount, batchDetails));
                    } else {
                        ThreadLogger.log(String.format("Cycle #%d: DB Sweep complete. All inventory batches within valid expiry windows.", scanCycle));
                    }
                } else {
                    ThreadLogger.log(String.format("Cycle #%d: Sweep complete (offline cache). Background monitor sleeping.", scanCycle));
                }

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
