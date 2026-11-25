package com.medistock.controller;

import com.medistock.AppConfig;
import com.medistock.concurrency.ThreadInfoSnapshot;
import com.medistock.concurrency.ThreadLogger;
import com.medistock.concurrency.ThreadManager;
import com.medistock.model.Pharmacy;
import com.medistock.model.User;
import com.medistock.service.PharmacyService;
import com.medistock.util.NavigationManager;
import com.medistock.util.SessionContext;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import org.example.medistock.controller.exchange.ExchangeController;
import org.example.medistock.controller.expiry.ExpiryController;
import org.example.medistock.controller.notifications.NotificationsController;
import org.example.medistock.service.expiry.ExpiryService;
import org.example.medistock.repository.notification.NotificationRepository;
import java.io.IOException;
import java.net.URL;

/**
 * Main application dashboard controller.
 * Manages system overview, KPI metrics, live thread monitoring widgets,
 * partner network display, and navigation between all modules.
 * Owner: Member 1.
 */
public class DashboardController {

    // Sidebar & User Badges
    @FXML
    private Label sidebarPharmacyName;
    @FXML
    private Label sidebarUserRole;
    @FXML
    private Label sidebarLicenseBadge;

    // Navigation Buttons
    @FXML
    private Button btnNavDashboard;
    @FXML
    private Button btnNavProfile;
    @FXML
    private Button btnNavThreadMonitor;
    @FXML
    private Button btnNavInventory;
    @FXML
    private Button btnNavMarketplace;
    @FXML
    private Button btnNavRequests;
    @FXML
    private Button btnNavExpiry;
    @FXML
    private Button btnNavNotifications;
    @FXML
    private Button btnNavReports;

    // Content Area Container
    @FXML
    private StackPane contentArea;
    @FXML
    private VBox dashboardHomeContent;

    // Dashboard Home View Elements
    @FXML
    private Label welcomeHeader;
    @FXML
    private Label welcomeSubheader;
    @FXML
    private Label metricTotalStock;
    @FXML
    private Label metricNearExpiry;
    @FXML
    private Label metricPendingRequests;
    @FXML
    private Label metricActiveThreads;
    @FXML
    private Label daemonStatusMini;

    // Partner Pharmacies Table
    @FXML
    private TableView<Pharmacy> partnerPharmaciesTable;
    @FXML
    private TableColumn<Pharmacy, String> colPartnerName;
    @FXML
    private TableColumn<Pharmacy, String> colPartnerLicense;
    @FXML
    private TableColumn<Pharmacy, String> colPartnerCity;
    @FXML
    private TableColumn<Pharmacy, Integer> colPartnerStock;
    @FXML
    private TableColumn<Pharmacy, Integer> colPartnerListings;

    // Live Activity Log Table
    @FXML
    private TableView<ThreadLogger.LogEntry> liveLogTable;
    @FXML
    private TableColumn<ThreadLogger.LogEntry, String> colLogTime;
    @FXML
    private TableColumn<ThreadLogger.LogEntry, String> colLogThread;
    @FXML
    private TableColumn<ThreadLogger.LogEntry, String> colLogState;
    @FXML
    private TableColumn<ThreadLogger.LogEntry, String> colLogMessage;

    private final PharmacyService pharmacyService = PharmacyService.getInstance();
    private final ThreadManager threadManager = ThreadManager.getInstance();
    private Timeline liveUpdateTimeline;

    // View caching for instant Member 4 navigation
    private Parent cachedExpiryView;
    private ExpiryController cachedExpiryController;
    private Parent cachedNotificationsView;
    private NotificationsController cachedNotificationsController;

    @FXML
    private void initialize() {
        populateUserSession();
        setupPartnerTable();
        setupLiveLogTable();
        refreshKpiMetrics();

        // Asynchronously prewarm caches for Expiry & Notifications so clicks are 0ms instant
        new Thread(() -> {
            try {
                ExpiryService.getInstance().fetchBatchesFromDB("PHARM-001");
                NotificationRepository.getInstance().findByPharmacyId("PHARM-001");
            } catch (Exception e) {
                System.err.println("[DashboardController] Cache prewarm error: " + e.getMessage());
            }
        }, "Member4-CachePrewarm").start();

        // Ensure default daemon thread is running for background observation
        if (!threadManager.isDaemonExpiryMonitorRunning()) {
            threadManager.startDaemonExpiryMonitor();
        }

        // Live status refresh ticker (every 2 seconds)
        liveUpdateTimeline = new Timeline(new KeyFrame(Duration.millis(2000), e -> {
            refreshKpiMetrics();
        }));
        liveUpdateTimeline.setCycleCount(Timeline.INDEFINITE);
        liveUpdateTimeline.play();

        highlightActiveNavButton(btnNavDashboard);
    }

    private void populateUserSession() {
        Pharmacy p = SessionContext.getCurrentPharmacy();
        User u = SessionContext.getCurrentUser();

        if (p != null) {
            sidebarPharmacyName.setText(p.getName());
            sidebarLicenseBadge.setText("License: " + p.getLicenseNumber() + " (" + p.getCity() + ")");
            welcomeSubheader.setText("Operating licensed branch at " + p.getAddress() + ", " + p.getCity());
        }

        if (u != null) {
            sidebarUserRole.setText(u.getFullName() + " | " + u.getRole());
            welcomeHeader.setText("Welcome back, " + u.getFullName());
        }
    }

    private void setupPartnerTable() {
        colPartnerName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPartnerLicense.setCellValueFactory(new PropertyValueFactory<>("licenseNumber"));
        colPartnerCity.setCellValueFactory(new PropertyValueFactory<>("city"));
        colPartnerStock.setCellValueFactory(new PropertyValueFactory<>("totalInventoryItems"));
        colPartnerListings.setCellValueFactory(new PropertyValueFactory<>("activeListingCount"));

        ObservableList<Pharmacy> partners = FXCollections.observableArrayList(pharmacyService.getAllPharmacies());
        partnerPharmaciesTable.setItems(partners);
    }

    private void setupLiveLogTable() {
        colLogTime.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        colLogThread.setCellValueFactory(new PropertyValueFactory<>("threadName"));
        colLogState.setCellValueFactory(new PropertyValueFactory<>("state"));
        colLogMessage.setCellValueFactory(new PropertyValueFactory<>("message"));

        liveLogTable.setItems(ThreadLogger.getObservableLogs());
    }

    private void refreshKpiMetrics() {
        Pharmacy p = SessionContext.getCurrentPharmacy();
        if (p != null) {
            metricTotalStock.setText(String.valueOf(p.getTotalInventoryItems()));
            metricNearExpiry.setText(String.valueOf(p.getNearExpiryCount()));
            metricPendingRequests.setText(String.valueOf(p.getActiveListingCount()));
        }

        int activeThreads = threadManager.getActiveThreadCount();
        metricActiveThreads.setText(String.valueOf(activeThreads));

        boolean daemonRunning = threadManager.isDaemonExpiryMonitorRunning();
        daemonStatusMini.setText(daemonRunning ? "Daemon Monitor: ACTIVE" : "Daemon Monitor: STOPPED");
        daemonStatusMini.setStyle(daemonRunning ? "-fx-text-fill: #10b981;" : "-fx-text-fill: #f87171;");
    }

    // Navigation Handlers
    @FXML
    private void handleNavDashboard(ActionEvent event) {
        showDashboardHome();
        highlightActiveNavButton(btnNavDashboard);
    }

    @FXML
    private void handleNavProfile(ActionEvent event) {
        loadSubView(AppConfig.PROFILE_VIEW);
        highlightActiveNavButton(btnNavProfile);
    }

    @FXML
    private void handleNavThreadMonitor(ActionEvent event) {
        loadSubView(AppConfig.THREAD_MONITOR_VIEW);
        highlightActiveNavButton(btnNavThreadMonitor);
    }

    @FXML
    private void handleNavInventory(ActionEvent event) {
        String fxmlPath =
                "/org/example/medistock/view/InventoryV/InventoryView.fxml";
        try {
            URL fxmlURL = getClass().getResource(fxmlPath);

            if (fxmlURL == null) {
                throw new IOException("FXML file not found at: " + fxmlPath);
            }

            FXMLLoader loader = new FXMLLoader(fxmlURL);
            Parent view = loader.load();

            contentArea.getChildren().setAll(view);

            highlightActiveNavButton(btnNavInventory);

            System.out.println("Inventory page loaded successfully!");

        } catch (Exception e) {

            System.err.println("Could not load Inventory page:");
            e.printStackTrace();

            showPlaceholder(
                    "My Inventory & Batch Management",
                    "Member 2 — Inventory Management",
                    "Central inventory management allowing pharmacies to manage medicine catalogs, batch numbers, manufacture & expiry dates, and real-time stock levels.",
                    "Owned Classes: InventoryController, Medicine, MedicineBatch, InventoryItem, InventoryService, InventoryRepository, StockManager, InventoryWorker",
                    "Multithreading Syllabus: Race conditions, critical sections, synchronized methods/blocks, intrinsic locks, atomicity, and visibility (volatile)."
            );

            highlightActiveNavButton(btnNavInventory);
        }
    }

    @FXML
    private void handleNavMarketplace(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(AppConfig.MARKETPLACE_VIEW));
            Parent view = loader.load();
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
            highlightActiveNavButton(btnNavMarketplace);
        } catch (Exception e) {
            e.printStackTrace();
            showPlaceholder(
                    "Stock Marketplace & Listings",
                    "Member 3 - Stock Exchange & Requests",
                    "Marketplace allowing pharmacies to browse surplus medicine batches from other licensed facilities and publish surplus listings with discounted unit rates.",
                    "Owned Classes: MarketplaceController, RequestsController, ExchangeListing, ExchangeRequest, StockTransfer, ExchangeService, ExchangeRepository, EventProducer, ExchangeWorker",
                    "Multithreading Syllabus: Concurrent exchange requests, stock reservation coordination, event production, shared-resource access, and notification queue dispatch."
            );
            highlightActiveNavButton(btnNavMarketplace);
        }
    }

    @FXML
    private void handleNavRequests(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(AppConfig.REQUESTS_VIEW));
            Parent view = loader.load();
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
            highlightActiveNavButton(btnNavRequests);
        } catch (Exception e) {
            e.printStackTrace();
            showPlaceholder(
                    "Exchange Requests & Transfer Tracking",
                    "Member 3 - Stock Exchange & Requests",
                    "Track inbound and outbound medicine transfer requests, approve or reject stock reservations, and track secure medicine transfers.",
                    "Owned Classes: RequestsController, ExchangeRequest, StockTransfer, ExchangeService",
                    "Multithreading Syllabus: Multi-pharmacy concurrent reservation locks and transactional consistency."
            );
            highlightActiveNavButton(btnNavRequests);
        }
    }



    @FXML
    private void handleNavExpiry(ActionEvent event) {
        try {
            if (cachedExpiryView == null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(AppConfig.EXPIRY_VIEW));
                cachedExpiryView = loader.load();
                cachedExpiryController = loader.getController();
            }
            contentArea.getChildren().clear();
            contentArea.getChildren().add(cachedExpiryView);
            highlightActiveNavButton(btnNavExpiry);
            if (cachedExpiryController != null) {
                cachedExpiryController.loadRecords();
                cachedExpiryController.loadRecordsAsync();
            }
        } catch (Exception e) {
            e.printStackTrace();
            showPlaceholder(
                    "Expiry Monitoring & Notifications",
                    "Member 4 — Expiry, Notifications & Reports",
                    "Real-time monitoring of medicine batches nearing expiry with automatic threshold alerts and notifications.",
                    "Owned Classes: ExpiryController, Notification, AppEvent, ExpiryService, NotificationService, CustomEventQueue, EventConsumer, NotificationWorker",
                    "Multithreading Syllabus: wait(), notify(), notifyAll(), guarded blocks with while loops, producer-consumer pattern, and custom bounded event queue."
            );
            highlightActiveNavButton(btnNavExpiry);
        }
    }

    @FXML
    private void handleNavNotifications(ActionEvent event) {
        try {
            if (cachedNotificationsView == null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(AppConfig.NOTIFICATIONS_VIEW));
                cachedNotificationsView = loader.load();
                cachedNotificationsController = loader.getController();
            }
            contentArea.getChildren().clear();
            contentArea.getChildren().add(cachedNotificationsView);
            highlightActiveNavButton(btnNavNotifications);
            if (cachedNotificationsController != null) {
                cachedNotificationsController.refreshData();
                cachedNotificationsController.refreshDataAsync();
            }
        } catch (Exception e) {
            e.printStackTrace();
            showPlaceholder(
                    "Centralized Notifications Feed",
                    "Member 4 — Expiry, Notifications & Reports",
                    "Real-time alerts, system notifications, and asynchronous batch expiry status updates.",
                    "Owned Classes: NotificationsController, NotificationService, NotificationRepository, NotificationWorker",
                    "Multithreading Syllabus: Asynchronous background worker threads and thread-safe notification persistence."
            );
            highlightActiveNavButton(btnNavNotifications);
        }
    }

    @FXML
    private void handleNavReports(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(AppConfig.REPORTS_VIEW));
            Parent view = loader.load();
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
            highlightActiveNavButton(btnNavReports);
        } catch (Exception e) {
            e.printStackTrace();
            showPlaceholder(
                    "Reports & Asynchronous Event Queue Monitor",
                    "Member 4 — Expiry, Notifications & Reports",
                    "Asynchronous bounded event queue metrics, consumer worker tracking, and pharmacy transfer reporting.",
                    "Owned Classes: ReportsController, CustomEventQueue, EventConsumer, NotificationWorker",
                    "Multithreading Syllabus: Custom bounded blocking queue with wait/notify coordination and safe thread shutdown strategy."
            );
            highlightActiveNavButton(btnNavReports);
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        if (liveUpdateTimeline != null) {
            liveUpdateTimeline.stop();
        }
        SessionContext.clearSession();
        NavigationManager.switchScene(event, AppConfig.LOGIN_VIEW);
    }

    // Quick Dashboard Actions
    @FXML
    private void handleQuickRunReport(ActionEvent event) {
        threadManager.spawnReportWorker("Audit Reconciliation", Thread.NORM_PRIORITY + 2); // Priority 7
        refreshKpiMetrics();
    }

    @FXML
    private void handleQuickToggleDaemon(ActionEvent event) {
        if (threadManager.isDaemonExpiryMonitorRunning()) {
            threadManager.stopDaemonExpiryMonitor();
        } else {
            threadManager.startDaemonExpiryMonitor();
        }
        refreshKpiMetrics();
    }

    private void showDashboardHome() {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(dashboardHomeContent);
        refreshKpiMetrics();
    }

    private void loadSubView(String fxmlPath) {
        try {
            Parent view = NavigationManager.loadView(fxmlPath);
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showPlaceholder(String title, String owner, String description, String classes, String concurrencyTopic) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(AppConfig.PLACEHOLDER_VIEW));
            Parent view = loader.load();
            PlaceholderController controller = loader.getController();
            controller.setModuleInfo(title, owner, description, classes, concurrencyTopic);

            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void highlightActiveNavButton(Button activeButton) {
        Button[] buttons = {
                btnNavDashboard, btnNavProfile, btnNavThreadMonitor,
                btnNavInventory, btnNavMarketplace, btnNavRequests,
                btnNavExpiry, btnNavNotifications
        };
        for (Button b : buttons) {
            if (b != null) {
                b.getStyleClass().remove("nav-button-active");
            }
        }
        if (activeButton != null && !activeButton.getStyleClass().contains("nav-button-active")) {
            activeButton.getStyleClass().add("nav-button-active");
        }
    }
}
