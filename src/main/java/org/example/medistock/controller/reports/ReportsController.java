package org.example.medistock.controller.reports;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.example.medistock.concurrency.CustomEventQueue;
import org.example.medistock.concurrency.EventConsumer;
import org.example.medistock.concurrency.ExpiryMonitorthread;
import org.example.medistock.concurrency.NotificationWorker;
import org.example.medistock.model.AppEvent;
import org.example.medistock.model.ExpiryRecord;
import org.example.medistock.service.expiry.ExpiryService;
import org.example.medistock.service.reports.ReportsService;

import java.net.URL;
import java.util.*;

/**
 * Controller for Inventory Expiry Reports and Concurrency Analytics.
 * Folder: controller/reports/
 * Owned by Member 4 (Expiry, Notifications & Reports).
 */
public class ReportsController implements Initializable {

    // KPI Labels
    @FXML private Label lblHealthScore;
    @FXML private Label lblTotalBatches;
    @FXML private Label lblTotalUnits;
    @FXML private Label lblAtRiskValue;
    @FXML private Label lblAtRiskBatches;
    @FXML private Label lblQueueLoad;

    // Concurrency Controls
    @FXML private Button btnToggleDaemon;
    @FXML private ProgressBar queueProgressBar;
    @FXML private Label lblQueuePercent;
    @FXML private Label lblEventsProcessed;

    // Risk Table
    @FXML private TableView<RiskRow> riskTableView;
    @FXML private TableColumn<RiskRow, String> colRiskCategory;
    @FXML private TableColumn<RiskRow, Integer> colRiskBatches;
    @FXML private TableColumn<RiskRow, Integer> colRiskUnits;
    @FXML private TableColumn<RiskRow, String> colRiskValue;
    @FXML private TableColumn<RiskRow, String> colRiskAction;

    // Thread Inspection Table
    @FXML private TableView<ThreadRow> threadTableView;
    @FXML private TableColumn<ThreadRow, String> colThreadName;
    @FXML private TableColumn<ThreadRow, String> colThreadType;
    @FXML private TableColumn<ThreadRow, Integer> colThreadPriority;
    @FXML private TableColumn<ThreadRow, String> colThreadState;
    @FXML private TableColumn<ThreadRow, String> colThreadRole;

    private final ReportsService reportsService = ReportsService.getInstance();
    private final ExpiryService expiryService = ExpiryService.getInstance();
    private final CustomEventQueue eventQueue = CustomEventQueue.getInstance();
    private final EventConsumer eventConsumer = EventConsumer.getInstance();
    private final ExpiryMonitorthread expiryMonitor = ExpiryMonitorthread.getInstance();
    private final NotificationWorker notificationWorker = NotificationWorker.getInstance();

    private final ObservableList<RiskRow> riskList = FXCollections.observableArrayList();
    private final ObservableList<ThreadRow> threadList = FXCollections.observableArrayList();
    private final String currentPharmacyId = "PHARM-001";

    public static class RiskRow {
        private final String category;
        private final int batches;
        private final int units;
        private final double value;
        private final String action;

        public RiskRow(String category, int batches, int units, double value, String action) {
            this.category = category;
            this.batches = batches;
            this.units = units;
            this.value = value;
            this.action = action;
        }

        public String getCategory() { return category; }
        public int getBatches() { return batches; }
        public int getUnits() { return units; }
        public String getValue() { return String.format("$%.2f", value); }
        public String getAction() { return action; }
    }

    public static class ThreadRow {
        private final String name;
        private final String type;
        private final int priority;
        private final String state;
        private final String role;

        public ThreadRow(String name, String type, int priority, String state, String role) {
            this.name = name;
            this.type = type;
            this.priority = priority;
            this.state = state;
            this.role = role;
        }

        public String getName() { return name; }
        public String getType() { return type; }
        public int getPriority() { return priority; }
        public String getState() { return state; }
        public String getRole() { return role; }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTables();
        refreshReport();
    }

    private void setupTables() {
        // Risk Table
        colRiskCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colRiskBatches.setCellValueFactory(new PropertyValueFactory<>("batches"));
        colRiskUnits.setCellValueFactory(new PropertyValueFactory<>("units"));
        colRiskValue.setCellValueFactory(new PropertyValueFactory<>("value"));
        colRiskAction.setCellValueFactory(new PropertyValueFactory<>("action"));
        riskTableView.setItems(riskList);

        // Thread Table
        colThreadName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colThreadType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colThreadPriority.setCellValueFactory(new PropertyValueFactory<>("priority"));
        colThreadState.setCellValueFactory(new PropertyValueFactory<>("state"));
        colThreadRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        threadTableView.setItems(threadList);
    }

    public void refreshReport() {
        ReportsService.InventoryHealthReport report = reportsService.generateReport(currentPharmacyId);

        // KPI updates
        if (lblHealthScore != null) lblHealthScore.setText(String.format("%.1f%%", report.healthScorePercent));
        if (lblTotalBatches != null) lblTotalBatches.setText(String.valueOf(report.totalBatches));
        if (lblTotalUnits != null) lblTotalUnits.setText(String.format("%d total stock units", report.totalUnits));
        if (lblAtRiskValue != null) lblAtRiskValue.setText(String.format("$%.2f", report.atRiskValue));
        if (lblAtRiskBatches != null) {
            int atRiskCount = report.expiredCount + report.criticalCount + report.nearExpiryCount;
            lblAtRiskBatches.setText(String.format("%d batches requiring action", atRiskCount));
        }

        // Event Queue Progress Bar
        int size = eventQueue.size();
        int capacity = eventQueue.getCapacity();
        double ratio = capacity > 0 ? (double) size / capacity : 0.0;
        if (lblQueueLoad != null) lblQueueLoad.setText(String.format("%d / %d", size, capacity));
        if (queueProgressBar != null) queueProgressBar.setProgress(ratio);
        if (lblQueuePercent != null) lblQueuePercent.setText(String.format("%.1f%% buffer load", ratio * 100));
        if (lblEventsProcessed != null) lblEventsProcessed.setText("Total Events Consumed: " + eventConsumer.getProcessedCount());

        if (btnToggleDaemon != null) {
            btnToggleDaemon.setText(expiryMonitor.isMonitoring() ? "⏸ Pause Daemon" : "▶ Resume Daemon");
        }

        populateRiskTable();
        populateThreadTable();
    }

    private void populateRiskTable() {
        List<ExpiryRecord> records = expiryService.getAllExpiryRecords(currentPharmacyId);

        int expBatches = 0, expUnits = 0; double expVal = 0;
        int critBatches = 0, critUnits = 0; double critVal = 0;
        int nearBatches = 0, nearUnits = 0; double nearVal = 0;
        int attBatches = 0, attUnits = 0; double attVal = 0;
        int safeBatches = 0, safeUnits = 0; double safeVal = 0;

        for (ExpiryRecord r : records) {
            switch (r.getStatus()) {
                case EXPIRED -> { expBatches++; expUnits += r.getQuantity(); expVal += r.getTotalValue(); }
                case CRITICAL -> { critBatches++; critUnits += r.getQuantity(); critVal += r.getTotalValue(); }
                case EXPIRING_SOON -> { nearBatches++; nearUnits += r.getQuantity(); nearVal += r.getTotalValue(); }
                case ATTENTION -> { attBatches++; attUnits += r.getQuantity(); attVal += r.getTotalValue(); }
                case SAFE -> { safeBatches++; safeUnits += r.getQuantity(); safeVal += r.getTotalValue(); }
            }
        }

        riskList.setAll(List.of(
                new RiskRow("EXPIRED (Past Date)", expBatches, expUnits, expVal, "Quarantine immediately; prohibit dispensing; initiate safe disposal"),
                new RiskRow("CRITICAL (<= 15 Days)", critBatches, critUnits, critVal, "Urgent surplus listing on marketplace with maximum discount; prioritize dispensing"),
                new RiskRow("NEAR-EXPIRY (<= 30 Days)", nearBatches, nearUnits, nearVal, "Publish to inter-pharmacy exchange; rotate stock to front shelves"),
                new RiskRow("ATTENTION (<= 60 Days)", attBatches, attUnits, attVal, "Monitor consumption rate; evaluate upcoming bulk prescription demands"),
                new RiskRow("SAFE (> 60 Days)", safeBatches, safeUnits, safeVal, "Optimal condition; normal standard storage & routine monitoring")
        ));
    }

    private void populateThreadTable() {
        List<ThreadRow> list = new ArrayList<>();

        list.add(new ThreadRow(
                "MediStock-ExpiryMonitor-Daemon",
                "Daemon",
                expiryMonitor.getPriority(),
                expiryMonitor.getState().name(),
                "Member 4: Continuous background inventory sweep (Periodic polling every 5000ms)"
        ));

        list.add(new ThreadRow(
                "EventConsumer-Thread",
                "Daemon",
                Thread.NORM_PRIORITY,
                eventConsumer.getThreadState().name(),
                "Member 4: Guarded block wait() consumer on CustomEventQueue for inter-member exchange events"
        ));

        list.add(new ThreadRow(
                "NotificationWorker-Thread",
                "Daemon",
                Thread.NORM_PRIORITY - 1,
                notificationWorker.isRunning() ? "RUNNABLE / WAITING" : "TERMINATED",
                "Member 4: Asynchronous JDBC database persistence of generated alerts without UI lag"
        ));

        list.add(new ThreadRow(
                "EventProducer-Thread",
                "Daemon",
                Thread.NORM_PRIORITY,
                "TIMED_WAITING / RUNNABLE",
                "Member 3: Produces AppEvents when listings or transfers change; notifies CustomEventQueue"
        ));

        list.add(new ThreadRow(
                "JavaFX Application Thread",
                "User (Main)",
                Thread.MAX_PRIORITY,
                "RUNNABLE",
                "All Members: Renders UI, responds to user mouse/keyboard input and dispatches background tasks"
        ));

        threadList.setAll(list);
    }

    @FXML
    private void handleProduceEvent(ActionEvent event) {
        String id = "EVT-MANUAL-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        AppEvent evt = new AppEvent(
                id,
                AppEvent.EventType.LISTING_CREATED,
                "PHARM-002",
                currentPharmacyId,
                "LST-SIM-" + System.currentTimeMillis() % 1000,
                "Partner facility published surplus listing: 50 units of Paracetamol (Simulated Producer Event)"
        );

        try {
            eventQueue.put(evt);
            System.out.println("[ReportsController] Produced event to CustomEventQueue: " + id);
            Thread.sleep(80);
            refreshReport();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @FXML
    private void handleConsumeEvent(ActionEvent event) {
        AppEvent taken = eventQueue.poll();
        if (taken != null) {
            System.out.println("[ReportsController] Manually polled event from CustomEventQueue: " + taken.getEventId());
        }
        refreshReport();
    }

    @FXML
    private void handleToggleDaemon(ActionEvent event) {
        if (expiryMonitor.isMonitoring()) {
            expiryMonitor.stopMonitoring();
        } else {
            expiryMonitor.startMonitoring();
        }
        refreshReport();
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        refreshReport();
    }
}
