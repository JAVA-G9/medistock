package com.medistock.controller;

import com.medistock.concurrency.ThreadInfoSnapshot;
import com.medistock.concurrency.ThreadLogger;
import com.medistock.concurrency.ThreadManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Duration;

/**
 * Controller for the Thread Monitor & Concurrency Lab screen.
 * Demonstrates: Thread lifecycle (NEW, RUNNABLE, BLOCKED, WAITING, TIMED_WAITING, TERMINATED),
 * daemon threads, priorities, thread naming, and execution tracing.
 * Owner: Member 1.
 */
public class ThreadMonitorController {

    // Thread Inspection Table
    @FXML
    private TableView<ThreadInfoSnapshot> threadTable;
    @FXML
    private TableColumn<ThreadInfoSnapshot, Long> colId;
    @FXML
    private TableColumn<ThreadInfoSnapshot, String> colName;
    @FXML
    private TableColumn<ThreadInfoSnapshot, Integer> colPriority;
    @FXML
    private TableColumn<ThreadInfoSnapshot, Boolean> colDaemon;
    @FXML
    private TableColumn<ThreadInfoSnapshot, String> colState;
    @FXML
    private TableColumn<ThreadInfoSnapshot, Boolean> colAlive;
    @FXML
    private TableColumn<ThreadInfoSnapshot, String> colPurpose;

    // Execution Trace Log Table
    @FXML
    private TableView<ThreadLogger.LogEntry> logTable;
    @FXML
    private TableColumn<ThreadLogger.LogEntry, String> colLogTime;
    @FXML
    private TableColumn<ThreadLogger.LogEntry, String> colLogThread;
    @FXML
    private TableColumn<ThreadLogger.LogEntry, Integer> colLogPriority;
    @FXML
    private TableColumn<ThreadLogger.LogEntry, String> colLogState;
    @FXML
    private TableColumn<ThreadLogger.LogEntry, String> colLogMessage;

    // Status / Control elements
    @FXML
    private Label daemonStatusLabel;
    @FXML
    private Button daemonToggleButton;
    @FXML
    private Label activeThreadsLabel;

    private final ThreadManager threadManager = ThreadManager.getInstance();
    private Timeline autoRefreshTimeline;

    @FXML
    private void initialize() {
        setupThreadTable();
        setupLogTable();
        updateDaemonUi();

        // Auto-refresh snapshot table every 1.5 seconds for live visualization
        autoRefreshTimeline = new Timeline(new KeyFrame(Duration.millis(1500), e -> {
            threadManager.refreshSnapshots();
            updateDaemonUi();
        }));
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimeline.play();

        // Initial refresh
        threadManager.refreshSnapshots();
    }

    private void setupThreadTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPriority.setCellValueFactory(new PropertyValueFactory<>("priority"));
        colDaemon.setCellValueFactory(new PropertyValueFactory<>("daemon"));
        colState.setCellValueFactory(new PropertyValueFactory<>("state"));
        colAlive.setCellValueFactory(new PropertyValueFactory<>("alive"));
        colPurpose.setCellValueFactory(new PropertyValueFactory<>("purpose"));

        threadTable.setItems(threadManager.getObservableSnapshots());
    }

    private void setupLogTable() {
        colLogTime.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        colLogThread.setCellValueFactory(new PropertyValueFactory<>("threadName"));
        colLogPriority.setCellValueFactory(new PropertyValueFactory<>("priority"));
        colLogState.setCellValueFactory(new PropertyValueFactory<>("state"));
        colLogMessage.setCellValueFactory(new PropertyValueFactory<>("message"));

        logTable.setItems(ThreadLogger.getObservableLogs());
    }

    private void updateDaemonUi() {
        boolean isRunning = threadManager.isDaemonExpiryMonitorRunning();
        if (daemonStatusLabel != null) {
            daemonStatusLabel.setText(isRunning ? "RUNNING (Daemon: true)" : "STOPPED");
            daemonStatusLabel.setStyle(isRunning
                    ? "-fx-text-fill: #059669; -fx-font-weight: bold;"
                    : "-fx-text-fill: #dc2626; -fx-font-weight: bold;");
        }
        if (daemonToggleButton != null) {
            daemonToggleButton.setText(isRunning ? "Stop Daemon Monitor" : "Start Daemon Monitor");
        }
        if (activeThreadsLabel != null) {
            activeThreadsLabel.setText("Active Tracked Threads: " + threadManager.getActiveThreadCount());
        }
    }

    @FXML
    private void handleToggleDaemon(ActionEvent event) {
        if (threadManager.isDaemonExpiryMonitorRunning()) {
            threadManager.stopDaemonExpiryMonitor();
        } else {
            threadManager.startDaemonExpiryMonitor();
        }
        updateDaemonUi();
    }

    @FXML
    private void handleDemonstrateSixStates(ActionEvent event) {
        threadManager.demonstrateAllSixStates();
        updateDaemonUi();
    }

    @FXML
    private void handleSpawnMaxPriorityWorker(ActionEvent event) {
        threadManager.spawnReportWorker("Critical Expiry Financial Risk Analysis", Thread.MAX_PRIORITY); // 10
        threadManager.refreshSnapshots();
    }

    @FXML
    private void handleSpawnNormPriorityWorker(ActionEvent event) {
        threadManager.spawnReportWorker("Standard Inventory Batch Reconciliation", Thread.NORM_PRIORITY); // 5
        threadManager.refreshSnapshots();
    }

    @FXML
    private void handleSpawnMinPriorityWorker(ActionEvent event) {
        threadManager.spawnReportWorker("Low-Priority Archival Log Backup", Thread.MIN_PRIORITY); // 1
        threadManager.refreshSnapshots();
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        threadManager.refreshSnapshots();
        updateDaemonUi();
    }

    @FXML
    private void handleClearLogs(ActionEvent event) {
        ThreadLogger.clear();
    }

    public void cleanup() {
        if (autoRefreshTimeline != null) {
            autoRefreshTimeline.stop();
        }
    }
}
