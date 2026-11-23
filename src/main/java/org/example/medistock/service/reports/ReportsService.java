package org.example.medistock.service.reports;

import org.example.medistock.concurrency.CustomEventQueue;
import org.example.medistock.concurrency.EventConsumer;
import org.example.medistock.concurrency.ExpiryMonitorthread;
import org.example.medistock.model.ExpiryRecord;
import org.example.medistock.service.expiry.ExpiryService;
import org.example.medistock.service.notifications.NotificationService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for generating comprehensive inventory health reports,
 * expiry projections, and multithreading event queue monitoring metrics.
 * Owned by Member 4 (Expiry, Notifications & Reports).
 */
public class ReportsService {

    private static ReportsService instance;
    private final ExpiryService expiryService;
    private final NotificationService notificationService;
    private final CustomEventQueue eventQueue;
    private final EventConsumer eventConsumer;
    private final ExpiryMonitorthread expiryMonitor;

    public ReportsService() {
        this(
                ExpiryService.getInstance(),
                NotificationService.getInstance(),
                CustomEventQueue.getInstance(),
                EventConsumer.getInstance(),
                ExpiryMonitorthread.getInstance()
        );
    }

    public ReportsService(ExpiryService expiryService,
                          NotificationService notificationService,
                          CustomEventQueue eventQueue,
                          EventConsumer eventConsumer,
                          ExpiryMonitorthread expiryMonitor) {
        this.expiryService = expiryService;
        this.notificationService = notificationService;
        this.eventQueue = eventQueue;
        this.eventConsumer = eventConsumer;
        this.expiryMonitor = expiryMonitor;
    }

    public static synchronized ReportsService getInstance() {
        if (instance == null) {
            instance = new ReportsService();
        }
        return instance;
    }

    public static class InventoryHealthReport {
        public int totalBatches;
        public int expiredCount;
        public int criticalCount;
        public int nearExpiryCount;
        public int safeCount;
        public int totalUnits;
        public double totalInventoryValue;
        public double atRiskValue;
        public double healthScorePercent;
        public int unreadNotifications;
        public int queueSize;
        public int queueCapacity;
        public int eventsProcessed;
        public boolean isMonitorRunning;
        public boolean isConsumerRunning;
    }

    public InventoryHealthReport generateReport(String pharmacyId) {
        InventoryHealthReport r = new InventoryHealthReport();
        List<ExpiryRecord> records = expiryService.getAllExpiryRecords(pharmacyId);

        r.totalBatches = records.size();
        for (ExpiryRecord rec : records) {
            r.totalUnits += rec.getQuantity();
            r.totalInventoryValue += rec.getTotalValue();

            switch (rec.getStatus()) {
                case EXPIRED -> {
                    r.expiredCount++;
                    r.atRiskValue += rec.getTotalValue();
                }
                case CRITICAL -> {
                    r.criticalCount++;
                    r.atRiskValue += rec.getTotalValue();
                }
                case EXPIRING_SOON -> {
                    r.nearExpiryCount++;
                    r.atRiskValue += rec.getTotalValue() * 0.75;
                }
                case ATTENTION -> r.atRiskValue += rec.getTotalValue() * 0.25;
                case SAFE -> r.safeCount++;
            }
        }

        // Calculate health score: 100% minus penalties for expired & critical items
        if (r.totalBatches > 0) {
            double penalty = ((r.expiredCount * 30.0) + (r.criticalCount * 15.0) + (r.nearExpiryCount * 5.0)) / r.totalBatches;
            r.healthScorePercent = Math.max(0.0, Math.min(100.0, 100.0 - penalty));
        } else {
            r.healthScorePercent = 100.0;
        }

        r.unreadNotifications = notificationService.getUnreadCount(pharmacyId);
        r.queueSize = eventQueue.size();
        r.queueCapacity = eventQueue.getCapacity();
        r.eventsProcessed = eventConsumer.getProcessedCount();
        r.isMonitorRunning = expiryMonitor.isMonitoring();
        r.isConsumerRunning = eventConsumer.isRunning();

        return r;
    }

    public Map<String, Integer> getStatusBreakdown(String pharmacyId) {
        Map<String, Integer> map = new HashMap<>();
        List<ExpiryRecord> records = expiryService.getAllExpiryRecords(pharmacyId);
        for (ExpiryRecord r : records) {
            map.put(r.getStatus().name(), map.getOrDefault(r.getStatus().name(), 0) + 1);
        }
        return map;
    }
}
