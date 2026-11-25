package org.example.medistock.controller.expiry;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.example.medistock.concurrency.ExpiryMonitorthread;
import org.example.medistock.model.ExpiryRecord;
import org.example.medistock.service.expiry.ExpiryService;
import org.example.medistock.service.reports.ReportsService;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for Medicine Expiry Monitoring view.
 * Folder: controller/expiry/
 * Owned by Member 4 (Expiry, Notifications & Reports).
 * Coordinates batch table display, filtering, daemon status tracing, and immediate scans.
 */
public class ExpiryController implements Initializable, ExpiryMonitorthread.ScanListener {

    // Header & Badges
    @FXML private Label lblDaemonStatusPill;

    // KPI Labels
    @FXML private Label lblKpiNearExpiry;
    @FXML private Label lblKpiCritical;
    @FXML private Label lblKpiExpired;
    @FXML private Label lblKpiAtRiskValue;

    // Concurrency Daemon Widget
    @FXML private Label lblThreadState;
    @FXML private Label lblDaemonCycleInfo;
    @FXML private Button btnTriggerScan;
    @FXML private Button btnToggleDaemon;

    // Action Strip
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cmbStatusFilter;
    @FXML private Label lblTableCount;

    // TableView & Columns
    @FXML private TableView<ExpiryRecord> expiryTableView;
    @FXML private TableColumn<ExpiryRecord, String> colBatchNumber;
    @FXML private TableColumn<ExpiryRecord, String> colMedicineName;
    @FXML private TableColumn<ExpiryRecord, String> colExpiryDate;
    @FXML private TableColumn<ExpiryRecord, String> colDaysRemaining;
    @FXML private TableColumn<ExpiryRecord, Integer> colQuantity;
    @FXML private TableColumn<ExpiryRecord, Double> colUnitPrice;
    @FXML private TableColumn<ExpiryRecord, String> colTotalValue;
    @FXML private TableColumn<ExpiryRecord, String> colStatus;

    private final ExpiryService expiryService = ExpiryService.getInstance();
    private final ReportsService reportsService = ReportsService.getInstance();
    private final ExpiryMonitorthread monitorThread = ExpiryMonitorthread.getInstance();
    private final ObservableList<ExpiryRecord> recordsList = FXCollections.observableArrayList();

    private final String currentPharmacyId = "PHARM-001";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        setupFilters();

        // 1. Immediately render cached records (0ms instant display - no blank table wait!)
        applyFilters();
        updateKpis();

        // Register controller as listener for background daemon sweeps
        monitorThread.addListener(this);

        // Start daemon thread if not already active
        if (!monitorThread.isMonitoring()) {
            monitorThread.startMonitoring();
        }
        updateDaemonUiStatus();

        // 2. Fetch fresh updates asynchronously in the background
        loadRecordsAsync();
    }

    private void setupTable() {
        colBatchNumber.setCellValueFactory(new PropertyValueFactory<>("batchNumber"));
        colMedicineName.setCellValueFactory(new PropertyValueFactory<>("medicineName"));
        colExpiryDate.setCellValueFactory(new PropertyValueFactory<>("formattedExpiryDate"));
        colDaysRemaining.setCellValueFactory(new PropertyValueFactory<>("daysRemainingDisplay"));
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colUnitPrice.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        colTotalValue.setCellValueFactory(new PropertyValueFactory<>("formattedTotalValue"));

        // Custom styled pill badge for Status column
        colStatus.setCellValueFactory(new PropertyValueFactory<>("statusDisplayName"));
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    ExpiryRecord record = getTableView().getItems().get(getIndex());
                    Label badge = new Label(item);
                    badge.setStyle(record.getStatusBadgeStyle());
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        expiryTableView.setItems(recordsList);
    }

    private void setupFilters() {
        cmbStatusFilter.getItems().addAll("ALL", "EXPIRED", "CRITICAL", "EXPIRING_SOON", "ATTENTION", "SAFE");
        cmbStatusFilter.setValue("ALL");

        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        cmbStatusFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    public void loadRecords() {
        applyFilters();
        updateKpis();
        updateDaemonUiStatus();
    }

    /**
     * Loads records on a background thread so the UI remains responsive.
     * Uses Platform.runLater to push results back to the JavaFX thread.
     */
    public void loadRecordsAsync() {
        Thread worker = new Thread(() -> {
            String query = txtSearch.getText();
            String filter = cmbStatusFilter.getValue();
            List<ExpiryRecord> filtered = expiryService.filterRecords(currentPharmacyId, query, filter);
            List<ExpiryRecord> all = expiryService.getAllExpiryRecords(currentPharmacyId);
            ReportsService.InventoryHealthReport report = reportsService.generateReport(currentPharmacyId);

            Platform.runLater(() -> {
                recordsList.setAll(filtered);
                if (lblTableCount != null)
                    lblTableCount.setText(String.format("Showing %d of %d batches", filtered.size(), all.size()));
                if (lblKpiNearExpiry != null) lblKpiNearExpiry.setText(String.valueOf(report.nearExpiryCount));
                if (lblKpiCritical != null) lblKpiCritical.setText(String.valueOf(report.criticalCount));
                if (lblKpiExpired != null) lblKpiExpired.setText(String.valueOf(report.expiredCount));
                if (lblKpiAtRiskValue != null) lblKpiAtRiskValue.setText(String.format("$%.2f", report.atRiskValue));
                updateDaemonUiStatus();
            });
        }, "ExpiryController-LoadAsync");
        worker.setDaemon(true);
        worker.start();
    }

    private void applyFilters() {
        String query = txtSearch.getText();
        String filter = cmbStatusFilter.getValue();
        List<ExpiryRecord> filtered = expiryService.filterCachedRecords(currentPharmacyId, query, filter);
        recordsList.setAll(filtered);
        if (lblTableCount != null) {
            lblTableCount.setText(String.format("Showing %d of %d batches", filtered.size(), expiryService.getCachedExpiryRecordsOnly(currentPharmacyId).size()));
        }
    }

    private void updateKpis() {
        ReportsService.InventoryHealthReport report = reportsService.generateCachedReport(currentPharmacyId);
        if (lblKpiNearExpiry != null) lblKpiNearExpiry.setText(String.valueOf(report.nearExpiryCount));
        if (lblKpiCritical != null) lblKpiCritical.setText(String.valueOf(report.criticalCount));
        if (lblKpiExpired != null) lblKpiExpired.setText(String.valueOf(report.expiredCount));
        if (lblKpiAtRiskValue != null) lblKpiAtRiskValue.setText(String.format("$%.2f", report.atRiskValue));
    }

    private void updateDaemonUiStatus() {
        boolean isRunning = monitorThread.isMonitoring();
        if (lblDaemonStatusPill != null) {
            lblDaemonStatusPill.setText(isRunning ? "DAEMON ACTIVE" : "DAEMON PAUSED");
            lblDaemonStatusPill.setStyle(isRunning
                    ? "-fx-background-color: #d1fae5; -fx-text-fill: #065f46;"
                    : "-fx-background-color: #fee2e2; -fx-text-fill: #991b1b;");
        }
        if (lblThreadState != null) {
            lblThreadState.setText("State: " + monitorThread.getState().name());
        }
        if (lblDaemonCycleInfo != null) {
            lblDaemonCycleInfo.setText(String.format("Sweep Cycle: #%d | Flagged: %d items | Sweep Interval: %d ms",
                    monitorThread.getScanCycle(), monitorThread.getFlaggedCount(), monitorThread.getIntervalMillis()));
        }
        if (btnToggleDaemon != null) {
            btnToggleDaemon.setText(isRunning ? "⏸ Pause Monitor" : "▶ Resume Monitor");
        }
    }

    @FXML
    private void handleTriggerImmediateScan(ActionEvent event) {
        new Thread(() -> {
            List<ExpiryRecord> urgent = expiryService.checkAndProcessExpiries(currentPharmacyId);
            Platform.runLater(() -> {
                loadRecords();
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Expiry Scan Complete");
                alert.setHeaderText("Real-Time Batch Verification Finished");
                alert.setContentText(String.format("Scan processed %d tracked batches. Flagged %d critical or near-expiry batches.",
                        expiryService.getAllExpiryRecords(currentPharmacyId).size(), urgent.size()));
                alert.showAndWait();
            });
        }, "ImmediateScan-Worker").start();
    }

    @FXML
    private void handleToggleDaemon(ActionEvent event) {
        if (monitorThread.isMonitoring()) {
            monitorThread.stopMonitoring();
        } else {
            monitorThread.startMonitoring();
        }
        updateDaemonUiStatus();
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        if (lblTableCount != null) lblTableCount.setText("Refreshing...");
        expiryService.invalidateCache();
        loadRecordsAsync();
    }

    @FXML
    private void handleDisposeExpired(ActionEvent event) {
        List<ExpiryRecord> expired = recordsList.stream()
                .filter(r -> r.getStatus() == ExpiryRecord.ExpiryStatus.EXPIRED)
                .toList();

        if (expired.isEmpty()) {
            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("No Expired Stock");
            info.setHeaderText("Inventory Clean");
            info.setContentText("No expired batches found in the current view.");
            info.showAndWait();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Stock Disposal");
        confirm.setHeaderText("Quarantine and Dispose Expired Stock");
        confirm.setContentText(String.format("Are you sure you want to quarantine and dispose %d expired batches?", expired.size()));
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                for (ExpiryRecord rec : expired) {
                    expiryService.disposeBatch(rec.getBatchNumber());
                }
                expiryService.invalidateCache();
                loadRecordsAsync();
            }
        });
    }

    @Override
    public void onScanCompleted(int cycle, List<ExpiryRecord> atRiskRecords) {
        // Daemon sweep completed: update cycle counter & daemon widgets without firing duplicate DB queries
        Platform.runLater(this::updateDaemonUiStatus);
    }
}
