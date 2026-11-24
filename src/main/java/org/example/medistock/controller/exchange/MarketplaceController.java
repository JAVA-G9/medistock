package org.example.medistock.controller.exchange;

import com.medistock.model.Pharmacy;
import com.medistock.service.PharmacyService;
import com.medistock.util.SessionContext;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import org.example.medistock.model.ExchangeListing;
import org.example.medistock.service.exchange.ExchangeService;

import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller for the Marketplace screen.
 * Folder: controller/exchange/
 * Owned by Member 3 (Stock Exchange & Requests).
 * Styled according to MediStock Unified Design System.
 */
public class MarketplaceController implements Initializable {

    @FXML private ComboBox<Pharmacy> pharmacyComboBox;
    @FXML private TextField searchField;
    @FXML private TableView<ExchangeListing> listingsTable;
    @FXML private TableColumn<ExchangeListing, String> colListingId;
    @FXML private TableColumn<ExchangeListing, String> colMedicine;
    @FXML private TableColumn<ExchangeListing, String> colBatch;
    @FXML private TableColumn<ExchangeListing, String> colExpiry;
    @FXML private TableColumn<ExchangeListing, Integer> colTotalQty;
    @FXML private TableColumn<ExchangeListing, Integer> colReservedQty;
    @FXML private TableColumn<ExchangeListing, Integer> colAvailQty;
    @FXML private TableColumn<ExchangeListing, Double> colPrice;
    @FXML private TableColumn<ExchangeListing, String> colSeller;
    @FXML private TableColumn<ExchangeListing, String> colStatus;

    // Header Badges & KPI Stat Labels
    @FXML private Label badgeFacility;
    @FXML private Label kpiTotalListings;
    @FXML private Label kpiMyListings;
    @FXML private Label kpiReservedStock;

    @FXML private Label statusLabel;

    private final ExchangeService exchangeService = ExchangeService.getInstance();
    private final PharmacyService pharmacyService = PharmacyService.getInstance();
    private final ObservableList<ExchangeListing> listingsData = FXCollections.observableArrayList();

    private Pharmacy activePharmacy;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        exchangeService.ensureWorkerStarted();
        setupPharmacySelector();
        setupTableColumns();
        loadListings();

        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterListings(newVal));
        logMessage("Marketplace initialized. Active Pharmacy: " + getActivePharmacy().getName());
    }

    private void setupPharmacySelector() {
        List<Pharmacy> pharmacies = pharmacyService.getAllPharmacies();
        pharmacyComboBox.setItems(FXCollections.observableArrayList(pharmacies));

        Pharmacy sessionPharmacy = SessionContext.getCurrentPharmacy();
        if (sessionPharmacy != null) {
            pharmacyComboBox.setValue(sessionPharmacy);
            this.activePharmacy = sessionPharmacy;
        } else if (!pharmacies.isEmpty()) {
            Pharmacy preferred = pharmacies.stream()
                    .filter(p -> p.getId().equalsIgnoreCase("PHARM-003"))
                    .findFirst()
                    .orElse(pharmacies.get(0));
            pharmacyComboBox.setValue(preferred);
            this.activePharmacy = preferred;
        }

        pharmacyComboBox.setOnAction(event -> {
            Pharmacy selected = pharmacyComboBox.getValue();
            if (selected != null) {
                this.activePharmacy = selected;
                logMessage("Switched active pharmacy to: " + selected.getName());
                loadListings();
            }
        });
    }

    private Pharmacy getActivePharmacy() {
        if (activePharmacy != null) {
            return activePharmacy;
        }
        Pharmacy session = SessionContext.getCurrentPharmacy();
        if (session != null) {
            return session;
        }
        return new Pharmacy("PHARM-001", "Apex Care Pharmacy (Metro City)", "DL-APO-9821", "12 Park Avenue", "Delhi", "+91 98765 43210", "apollo@medistock.com");
    }

    private void setupTableColumns() {
        colListingId.setCellValueFactory(new PropertyValueFactory<>("listingId"));
        colMedicine.setCellValueFactory(new PropertyValueFactory<>("medicineName"));
        colBatch.setCellValueFactory(new PropertyValueFactory<>("batchNumber"));
        colExpiry.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getExpiryDate().toString()));
        colTotalQty.setCellValueFactory(new PropertyValueFactory<>("totalQuantity"));
        colReservedQty.setCellValueFactory(new PropertyValueFactory<>("reservedQuantity"));
        colAvailQty.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getAvailableQuantity()).asObject());
        colPrice.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        colSeller.setCellValueFactory(new PropertyValueFactory<>("sellerPharmacyName"));
        colStatus.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatus().name()));

        listingsTable.setItems(listingsData);
    }

    public void loadListings() {
        List<ExchangeListing> listings = exchangeService.getAllListings();
        listingsData.setAll(listings);

        Pharmacy cur = getActivePharmacy();
        long activeCount = listings.stream().filter(l -> l.getStatus() == ExchangeListing.ListingStatus.AVAILABLE).count();
        long myCount = listings.stream().filter(l -> l.getSellerPharmacyId().equalsIgnoreCase(cur.getId())).count();
        int reservedUnits = listings.stream().mapToInt(ExchangeListing::getReservedQuantity).sum();

        if (kpiTotalListings != null) {
            kpiTotalListings.setText(String.valueOf(activeCount));
        }
        if (kpiMyListings != null) {
            kpiMyListings.setText(String.valueOf(myCount));
        }
        if (kpiReservedStock != null) {
            kpiReservedStock.setText(reservedUnits + " units");
        }
        if (badgeFacility != null) {
            badgeFacility.setText("PHARMACY: " + cur.getName() + " (" + cur.getCity() + ")");
        }
        if (statusLabel != null) {
            statusLabel.setText("Showing " + listings.size() + " listings in marketplace.");
        }
    }

    private void filterListings(String query) {
        if (query == null || query.trim().isEmpty()) {
            loadListings();
        } else {
            List<ExchangeListing> filtered = exchangeService.searchListings(query);
            listingsData.setAll(filtered);
            if (statusLabel != null) {
                statusLabel.setText("Found " + filtered.size() + " matching listings.");
            }
        }
    }

    @FXML
    public void handleRefresh() {
        loadListings();
        logMessage("Marketplace listings refreshed.");
    }

    @FXML
    public void handlePublishListing() {
        Pharmacy currentPharmacy = getActivePharmacy();

        Dialog<ExchangeListing> dialog = new Dialog<>();
        dialog.setTitle("Publish Surplus Medicine Listing");
        dialog.setHeaderText("Add surplus medicine to the PharmaExchange Marketplace\nSeller: " + currentPharmacy.getName());

        ButtonType publishButtonType = new ButtonType("Publish Listing", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(publishButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField medNameField = new TextField();
        medNameField.setPromptText("e.g. Ciprofloxacin 500mg");
        medNameField.getStyleClass().add("input-field");

        TextField batchField = new TextField();
        batchField.setPromptText("e.g. CIP-2024-05");
        batchField.getStyleClass().add("input-field");

        DatePicker expiryPicker = new DatePicker(LocalDate.now().plusMonths(3));
        expiryPicker.getStyleClass().add("input-field");

        TextField qtyField = new TextField();
        qtyField.setPromptText("e.g. 100");
        qtyField.getStyleClass().add("input-field");

        TextField priceField = new TextField();
        priceField.setPromptText("e.g. 45.00");
        priceField.getStyleClass().add("input-field");

        TextField notesField = new TextField();
        notesField.setPromptText("e.g. Surplus batch, ready to dispatch");
        notesField.getStyleClass().add("input-field");

        grid.add(new Label("Medicine Name:"), 0, 0);
        grid.add(medNameField, 1, 0);
        grid.add(new Label("Batch Number:"), 0, 1);
        grid.add(batchField, 1, 1);
        grid.add(new Label("Expiry Date:"), 0, 2);
        grid.add(expiryPicker, 1, 2);
        grid.add(new Label("Total Quantity:"), 0, 3);
        grid.add(qtyField, 1, 3);
        grid.add(new Label("Unit Price (₹):"), 0, 4);
        grid.add(priceField, 1, 4);
        grid.add(new Label("Exchange Notes:"), 0, 5);
        grid.add(notesField, 1, 5);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == publishButtonType) {
                try {
                    String name = medNameField.getText().trim();
                    String batch = batchField.getText().trim();
                    LocalDate exp = expiryPicker.getValue();
                    int qty = Integer.parseInt(qtyField.getText().trim());
                    double price = Double.parseDouble(priceField.getText().trim());
                    String notes = notesField.getText().trim();

                    if (name.isEmpty() || batch.isEmpty() || exp == null) {
                        throw new IllegalArgumentException("All fields must be filled.");
                    }

                    return exchangeService.createListing(
                            currentPharmacy.getId(),
                            currentPharmacy.getName(),
                            name, batch, exp, qty, price, notes
                    );
                } catch (Exception e) {
                    showError("Invalid Input", e.getMessage());
                    return null;
                }
            }
            return null;
        });

        Optional<ExchangeListing> result = dialog.showAndWait();
        if (result.isPresent()) {
            ExchangeListing listing = result.get();
            loadListings();
            logMessage("SUCCESS: Published listing " + listing.getListingId() + " (" + listing.getMedicineName() + ")");
        }
    }

    @FXML
    public void handleRequestStock() {
        ExchangeListing selected = listingsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("No Selection", "Please select a listing from the table first.");
            return;
        }

        Pharmacy currentPharmacy = getActivePharmacy();

        if (selected.getSellerPharmacyId().equalsIgnoreCase(currentPharmacy.getId())) {
            showError("Invalid Request", "You cannot request stock from your own pharmacy's listing.");
            return;
        }

        if (selected.getAvailableQuantity() <= 0) {
            showError("Unavailable", "This listing has no stock available for reservation.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog("10");
        dialog.setTitle("Request Medicine Stock");
        dialog.setHeaderText(String.format("Requesting %s from %s\nAvailable: %d units | Unit Price: ₹%.2f",
                selected.getMedicineName(), selected.getSellerPharmacyName(),
                selected.getAvailableQuantity(), selected.getUnitPrice()));
        dialog.setContentText("Enter quantity to request:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String qtyStr = result.get();
            try {
                int qty = Integer.parseInt(qtyStr.trim());
                exchangeService.submitRequest(
                        selected.getListingId(),
                        currentPharmacy.getId(),
                        currentPharmacy.getName(),
                        qty,
                        selected.getUnitPrice(),
                        "Standard exchange stock request"
                );
                loadListings();
                logMessage(String.format("SUCCESS: Requested %d units of %s. Stock reserved atomically.",
                        qty, selected.getMedicineName()));
                showInfo("Request Placed", "Your request and stock reservation have been submitted to " + selected.getSellerPharmacyName());
            } catch (Exception e) {
                showError("Reservation Failed", e.getMessage());
                logMessage("RESERVATION FAILED: " + e.getMessage());
            }
        }
    }

    @FXML
    public void handleCancelListing() {
        ExchangeListing selected = listingsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("No Selection", "Please select a listing to cancel.");
            return;
        }

        Pharmacy currentPharmacy = getActivePharmacy();
        try {
            boolean success = exchangeService.cancelListing(selected.getListingId(), currentPharmacy.getId());
            if (success) {
                loadListings();
                logMessage("Cancelled listing: " + selected.getListingId());
            } else {
                showError("Error", "Could not cancel listing.");
            }
        } catch (Exception e) {
            showError("Cancellation Denied", e.getMessage());
            logMessage("CANCEL ERROR: " + e.getMessage());
        }
    }

    @FXML
    public void handleRunConcurrencyTest() {
        ExchangeListing selected = listingsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Select Listing", "Please select a listing to run the concurrent reservation test on.");
            return;
        }

        logMessage(String.format("Launching Concurrency Stress Test on %s (%d units available)...",
                selected.getMedicineName(), selected.getAvailableQuantity()));

        new Thread(() -> {
            try {
                // 6 concurrent threads requesting 20 units each
                String report = exchangeService.runConcurrentReservationTest(selected.getListingId(), 6, 20);
                Platform.runLater(() -> {
                    logMessage(report);
                    loadListings();
                    showInfo("Concurrency Test Completed", report);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Concurrency Test Error", e.getMessage());
                    logMessage("CONCURRENCY ERROR: " + e.getMessage());
                });
            }
        }, "ConcurrencyTest-Runner").start();
    }

    private void logMessage(String msg) {
        System.out.printf("[%s] %s%n", java.time.LocalTime.now().withNano(0), msg);
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
