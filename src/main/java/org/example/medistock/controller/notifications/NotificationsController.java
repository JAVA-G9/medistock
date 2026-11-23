package org.example.medistock.controller.notifications;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.example.medistock.concurrency.CustomEventQueue;
import org.example.medistock.concurrency.EventConsumer;
import org.example.medistock.model.AppEvent;
import org.example.medistock.model.Notification;
import org.example.medistock.service.notifications.NotificationService;
import org.example.medistock.service.reports.ReportsService;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;

/**
 * Controller for Centralized Notifications and Event Feed.
 * Folder: controller/notifications/
 * Owned by Member 4 (Expiry, Notifications & Reports).
 */
public class NotificationsController implements Initializable {

    // Badges & Widgets
    @FXML private Label lblConsumerStatusBadge;
    @FXML private Label lblConsumerState;
    @FXML private Label lblConsumerDetails;

    // KPI Labels
    @FXML private Label lblKpiUnread;
    @FXML private Label lblKpiHighPriority;
    @FXML private Label lblKpiTotal;
    @FXML private Label lblKpiQueueLoad;

    // Action Strip
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cmbTypeFilter;
    @FXML private Label lblNotificationCount;

    // TableView
    @FXML private TableView<Notification> notificationsTableView;
    @FXML private TableColumn<Notification, String> colTimestamp;
    @FXML private TableColumn<Notification, String> colPriority;
    @FXML private TableColumn<Notification, String> colType;
    @FXML private TableColumn<Notification, String> colTitle;
    @FXML private TableColumn<Notification, String> colMessage;
    @FXML private TableColumn<Notification, String> colStatus;

    private final NotificationService notificationService = NotificationService.getInstance();
    private final ReportsService reportsService = ReportsService.getInstance();
    private final CustomEventQueue eventQueue = CustomEventQueue.getInstance();
    private final EventConsumer eventConsumer = EventConsumer.getInstance();

    private final ObservableList<Notification> notificationList = FXCollections.observableArrayList();
    private final String currentPharmacyId = "PHARM-001";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        setupFilters();

        // Start consumer worker if not running
        if (!eventConsumer.isRunning()) {
            eventConsumer.start();
        }

        refreshData();
    }

    private void setupTable() {
        colTimestamp.setCellValueFactory(new PropertyValueFactory<>("formattedTimestamp"));
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colMessage.setCellValueFactory(new PropertyValueFactory<>("message"));

        // Priority Badge Cell Factory
        colPriority.setCellValueFactory(new PropertyValueFactory<>("priority"));
        colPriority.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Notification n = getTableView().getItems().get(getIndex());
                    Label pill = new Label(item);
                    pill.setStyle(n.getPriorityBadgeStyle());
                    setGraphic(pill);
                    setText(null);
                }
            }
        });

        // Type Badge Cell Factory
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colType.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Notification n = getTableView().getItems().get(getIndex());
                    Label pill = new Label(n.getType().name().replace('_', ' '));
                    pill.setStyle(n.getTypeBadgeStyle());
                    setGraphic(pill);
                    setText(null);
                }
            }
        });

        // Status Column
        colStatus.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().isRead() ? "Read" : "Unread"
        ));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label pill = new Label(item);
                    if ("Unread".equalsIgnoreCase(item)) {
                        pill.setStyle("-fx-background-color: #fef3c7; -fx-text-fill: #92400e; -fx-font-weight: bold; -fx-padding: 2 6; -fx-background-radius: 10;");
                    } else {
                        pill.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #64748b; -fx-padding: 2 6; -fx-background-radius: 10;");
                    }
                    setGraphic(pill);
                    setText(null);
                }
            }
        });

        // Click row to mark as read
        notificationsTableView.setRowFactory(tv -> {
            TableRow<Notification> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getClickCount() == 2) {
                    Notification clicked = row.getItem();
                    if (!clicked.isRead()) {
                        notificationService.markAsRead(clicked.getNotificationId());
                        clicked.setRead(true);
                        notificationsTableView.refresh();
                        updateKpis();
                    }
                }
            });
            return row;
        });

        notificationsTableView.setItems(notificationList);
    }

    private void setupFilters() {
        cmbTypeFilter.getItems().addAll("ALL", "EXPIRY_WARNING", "BATCH_EXPIRED", "EXCHANGE_EVENT", "LOW_STOCK", "SYSTEM_ALERT");
        cmbTypeFilter.setValue("ALL");

        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        cmbTypeFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    public void refreshData() {
        applyFilters();
        updateKpis();
        updateConsumerUi();
    }

    private void applyFilters() {
        String query = txtSearch != null ? txtSearch.getText().trim().toLowerCase() : "";
        String typeFilter = cmbTypeFilter != null ? cmbTypeFilter.getValue() : "ALL";

        List<Notification> all = notificationService.getNotifications(currentPharmacyId);
        List<Notification> filtered = all.stream().filter(n -> {
            boolean matchesQuery = query.isEmpty()
                    || n.getTitle().toLowerCase().contains(query)
                    || n.getMessage().toLowerCase().contains(query);

            boolean matchesType = typeFilter == null || "ALL".equalsIgnoreCase(typeFilter)
                    || n.getType().name().equalsIgnoreCase(typeFilter);

            return matchesQuery && matchesType;
        }).toList();

        notificationList.setAll(filtered);
        if (lblNotificationCount != null) {
            lblNotificationCount.setText(String.format("Showing %d of %d notifications", filtered.size(), all.size()));
        }
    }

    private void updateKpis() {
        ReportsService.InventoryHealthReport report = reportsService.generateReport(currentPharmacyId);
        List<Notification> all = notificationService.getNotifications(currentPharmacyId);

        long highPriorityCount = all.stream()
                .filter(n -> n.getPriority() == Notification.Priority.HIGH)
                .count();

        if (lblKpiUnread != null) lblKpiUnread.setText(String.valueOf(report.unreadNotifications));
        if (lblKpiHighPriority != null) lblKpiHighPriority.setText(String.valueOf(highPriorityCount));
        if (lblKpiTotal != null) lblKpiTotal.setText(String.valueOf(all.size()));
        if (lblKpiQueueLoad != null) lblKpiQueueLoad.setText(String.format("%d / %d", report.queueSize, report.queueCapacity));
    }

    private void updateConsumerUi() {
        boolean running = eventConsumer.isRunning();
        if (lblConsumerStatusBadge != null) {
            lblConsumerStatusBadge.setText(running ? "CONSUMER ACTIVE" : "CONSUMER STOPPED");
            lblConsumerStatusBadge.setStyle(running
                    ? "-fx-background-color: #d1fae5; -fx-text-fill: #065f46;"
                    : "-fx-background-color: #fee2e2; -fx-text-fill: #991b1b;");
        }
        if (lblConsumerState != null) {
            lblConsumerState.setText("State: " + eventConsumer.getThreadState().name());
        }
        if (lblConsumerDetails != null) {
            lblConsumerDetails.setText(String.format(
                    "Thread: EventConsumer-Thread | Guarded wait() on CustomEventQueue | Processed: %d events | Queue Buffer: %d/%d",
                    eventConsumer.getProcessedCount(), eventQueue.size(), eventQueue.getCapacity()
            ));
        }
    }

    @FXML
    private void handleProduceTestEvent(ActionEvent event) {
        String eventId = "EVT-TEST-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        AppEvent testEvent = new AppEvent(
                eventId,
                AppEvent.EventType.STOCK_RESERVED,
                "PHARM-002",
                currentPharmacyId,
                "LST-TEST-99",
                "Pharmacy 'MedPlus' reserved 30 units of Amoxicillin 500mg from surplus listing LST-TEST-99"
        );

        try {
            eventQueue.put(testEvent);
            System.out.println("[NotificationsController] Produced test event to CustomEventQueue: " + eventId);
            // Brief sleep to allow consumer thread to take and process
            Thread.sleep(100);
            refreshData();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @FXML
    private void handleTriggerTestAlert(ActionEvent event) {
        notificationService.triggerSampleAlert(currentPharmacyId);
        refreshData();
    }

    @FXML
    private void handleMarkAllRead(ActionEvent event) {
        notificationService.markAllAsRead(currentPharmacyId);
        refreshData();
    }

    @FXML
    private void handleClearRead(ActionEvent event) {
        List<Notification> all = notificationService.getNotifications(currentPharmacyId);
        for (Notification n : all) {
            if (n.isRead()) {
                notificationService.deleteNotification(n.getNotificationId());
            }
        }
        refreshData();
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        refreshData();
    }
}
