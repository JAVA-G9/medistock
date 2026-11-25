package org.example.medistock.controller.exchange;

import com.medistock.model.Pharmacy;
import com.medistock.service.PharmacyService;
import com.medistock.util.SessionContext;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.example.medistock.model.ExchangeRequest;
import org.example.medistock.model.StockTransfer;
import org.example.medistock.service.exchange.ExchangeService;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller for managing Exchange Requests and Stock Transfers.
 * Folder: controller/exchange/
 * Owned by Member 3 (Stock Exchange & Requests).
 * Styled according to MediStock Unified Design System.
 */
public class RequestsController implements Initializable {

    // Header Badges & KPI Stat Labels
    @FXML private Label badgeFacilityReq;
    @FXML private Label kpiIncomingPending;
    @FXML private Label kpiOutgoingRequests;
    @FXML private Label kpiActiveTransfers;

    // Incoming Requests Table
    @FXML private TableView<ExchangeRequest> incomingTable;
    @FXML private TableColumn<ExchangeRequest, String> colIncId;
    @FXML private TableColumn<ExchangeRequest, String> colIncListing;
    @FXML private TableColumn<ExchangeRequest, String> colIncRequester;
    @FXML private TableColumn<ExchangeRequest, String> colIncMedicine;
    @FXML private TableColumn<ExchangeRequest, Integer> colIncQty;
    @FXML private TableColumn<ExchangeRequest, Double> colIncPrice;
    @FXML private TableColumn<ExchangeRequest, Double> colIncTotal;
    @FXML private TableColumn<ExchangeRequest, String> colIncStatus;
    @FXML private TableColumn<ExchangeRequest, String> colIncDate;

    // Outgoing Requests Table
    @FXML private TableView<ExchangeRequest> outgoingTable;
    @FXML private TableColumn<ExchangeRequest, String> colOutId;
    @FXML private TableColumn<ExchangeRequest, String> colOutListing;
    @FXML private TableColumn<ExchangeRequest, String> colOutSeller;
    @FXML private TableColumn<ExchangeRequest, String> colOutMedicine;
    @FXML private TableColumn<ExchangeRequest, Integer> colOutQty;
    @FXML private TableColumn<ExchangeRequest, Double> colOutPrice;
    @FXML private TableColumn<ExchangeRequest, Double> colOutTotal;
    @FXML private TableColumn<ExchangeRequest, String> colOutStatus;
    @FXML private TableColumn<ExchangeRequest, String> colOutDate;

    // Stock Transfers Table
    @FXML private TableView<StockTransfer> transfersTable;
    @FXML private TableColumn<StockTransfer, String> colTrfId;
    @FXML private TableColumn<StockTransfer, String> colTrfMedicine;
    @FXML private TableColumn<StockTransfer, Integer> colTrfQty;
    @FXML private TableColumn<StockTransfer, String> colTrfSource;
    @FXML private TableColumn<StockTransfer, String> colTrfDest;
    @FXML private TableColumn<StockTransfer, String> colTrfTracking;
    @FXML private TableColumn<StockTransfer, String> colTrfStatus;
    @FXML private TableColumn<StockTransfer, String> colTrfCreated;

    @FXML private Label statusLabel;

    private final ExchangeService exchangeService = ExchangeService.getInstance();

    private final ObservableList<ExchangeRequest> incomingData = FXCollections.observableArrayList();
    private final ObservableList<ExchangeRequest> outgoingData = FXCollections.observableArrayList();
    private final ObservableList<StockTransfer> transfersData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        exchangeService.ensureWorkerStarted();
        setupIncomingTable();
        setupOutgoingTable();
        setupTransfersTable();
        refreshAllData();

        logMessage("Requests & Transfer Controller ready.");
    }

    private Pharmacy getActivePharmacy() {
        Pharmacy session = SessionContext.getCurrentPharmacy();
        if (session != null) {
            return session;
        }
        return new Pharmacy("PHARM-001", "Apex Care Pharmacy (Metro City)", "DL-APO-9821", "12 Park Avenue", "Delhi", "+91 98765 43210", "apollo@medistock.com");
    }

    private void setupIncomingTable() {
        colIncId.setCellValueFactory(new PropertyValueFactory<>("requestId"));
        colIncListing.setCellValueFactory(new PropertyValueFactory<>("listingId"));
        colIncRequester.setCellValueFactory(new PropertyValueFactory<>("requesterPharmacyName"));
        colIncMedicine.setCellValueFactory(new PropertyValueFactory<>("medicineName"));
        colIncQty.setCellValueFactory(new PropertyValueFactory<>("requestedQuantity"));
        colIncPrice.setCellValueFactory(new PropertyValueFactory<>("offeredPrice"));
        colIncTotal.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        colIncStatus.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatus().name()));
        colIncDate.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCreatedAt().toString().substring(0, 16)));

        incomingTable.setPlaceholder(new Label("No incoming requests pending for this facility's listings."));
        incomingTable.setItems(incomingData);
    }

    private void setupOutgoingTable() {
        colOutId.setCellValueFactory(new PropertyValueFactory<>("requestId"));
        colOutListing.setCellValueFactory(new PropertyValueFactory<>("listingId"));
        colOutSeller.setCellValueFactory(new PropertyValueFactory<>("sellerPharmacyId"));
        colOutMedicine.setCellValueFactory(new PropertyValueFactory<>("medicineName"));
        colOutQty.setCellValueFactory(new PropertyValueFactory<>("requestedQuantity"));
        colOutPrice.setCellValueFactory(new PropertyValueFactory<>("offeredPrice"));
        colOutTotal.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        colOutStatus.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatus().name()));
        colOutDate.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCreatedAt().toString().substring(0, 16)));

        outgoingTable.setPlaceholder(new Label("No outgoing requests placed by this facility."));
        outgoingTable.setItems(outgoingData);
    }

    private void setupTransfersTable() {
        colTrfId.setCellValueFactory(new PropertyValueFactory<>("transferId"));
        colTrfMedicine.setCellValueFactory(new PropertyValueFactory<>("medicineName"));
        colTrfQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colTrfSource.setCellValueFactory(new PropertyValueFactory<>("sourcePharmacyName"));
        colTrfDest.setCellValueFactory(new PropertyValueFactory<>("destinationPharmacyName"));
        colTrfTracking.setCellValueFactory(new PropertyValueFactory<>("trackingNumber"));
        colTrfStatus.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatus().name()));
        colTrfCreated.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCreatedAt().toString().substring(0, 16)));

        transfersTable.setPlaceholder(new Label("No stock transfers recorded for this facility yet."));
        transfersTable.setItems(transfersData);
    }

    public void refreshAllData() {
        Pharmacy cur = getActivePharmacy();
        List<ExchangeRequest> incoming = exchangeService.getRequestsForSeller(cur.getId());
        incomingData.setAll(incoming);
        if (incomingTable != null) {
            incomingTable.refresh();
        }

        List<ExchangeRequest> outgoing = exchangeService.getRequestsByRequester(cur.getId());
        outgoingData.setAll(outgoing);
        if (outgoingTable != null) {
            outgoingTable.refresh();
        }

        List<StockTransfer> transfers = exchangeService.getTransfersForPharmacy(cur.getId());
        transfersData.setAll(transfers);
        if (transfersTable != null) {
            transfersTable.refresh();
        }

        long pendingIncoming = incoming.stream().filter(r -> r.getStatus() == ExchangeRequest.RequestStatus.PENDING).count();

        if (kpiIncomingPending != null) {
            kpiIncomingPending.setText(String.valueOf(pendingIncoming));
        }
        if (kpiOutgoingRequests != null) {
            kpiOutgoingRequests.setText(String.valueOf(outgoing.size()));
        }
        if (kpiActiveTransfers != null) {
            kpiActiveTransfers.setText(String.valueOf(transfers.size()));
        }
        if (badgeFacilityReq != null) {
            badgeFacilityReq.setText("FACILITY: " + cur.getName() + " (" + cur.getLicenseNumber() + ")");
        }
        if (statusLabel != null) {
            statusLabel.setText(String.format("Incoming: %d | Outgoing: %d | Transfers: %d",
                    incoming.size(), outgoing.size(), transfers.size()));
        }
    }

    @FXML
    public void handleApproveRequest() {
        ExchangeRequest selected = incomingTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("No Selection", "Please select an incoming request to approve.");
            return;
        }

        if (selected.getStatus() != ExchangeRequest.RequestStatus.PENDING) {
            showError("Already Processed", "This request is already " + selected.getStatus());
            return;
        }

        Pharmacy currentPharmacy = getActivePharmacy();
        try {
            StockTransfer transfer = exchangeService.approveRequest(selected.getRequestId(), currentPharmacy.getId());
            refreshAllData();
            logMessage("APPROVED Request " + selected.getRequestId() + ". Created Transfer: " + transfer.getTransferId());
            showInfo("Request Approved", "Request approved! Stock Transfer " + transfer.getTransferId() + " has been initiated.");
        } catch (Exception e) {
            showError("Approval Error", e.getMessage());
            logMessage("APPROVE ERROR: " + e.getMessage());
        }
    }

    @FXML
    public void handleRejectRequest() {
        ExchangeRequest selected = incomingTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("No Selection", "Please select an incoming request to reject.");
            return;
        }

        if (selected.getStatus() != ExchangeRequest.RequestStatus.PENDING) {
            showError("Already Processed", "This request is already " + selected.getStatus());
            return;
        }

        TextInputDialog dialog = new TextInputDialog("Stock unavailable / pricing mismatch");
        dialog.setTitle("Reject Exchange Request");
        dialog.setHeaderText("Rejecting request " + selected.getRequestId() + " for " + selected.getMedicineName());
        dialog.setContentText("Enter rejection reason:");

        Optional<String> reasonOpt = dialog.showAndWait();
        if (reasonOpt.isPresent()) {
            String reason = reasonOpt.get();
            try {
                Pharmacy currentPharmacy = getActivePharmacy();
                exchangeService.rejectRequest(selected.getRequestId(), currentPharmacy.getId(), reason);
                refreshAllData();
                logMessage("REJECTED Request " + selected.getRequestId() + ". Stock reservation released.");
                showInfo("Request Rejected", "The request has been rejected and the reserved stock released back to available inventory.");
            } catch (Exception e) {
                showError("Rejection Error", e.getMessage());
                logMessage("REJECT ERROR: " + e.getMessage());
            }
        }
    }

    @FXML
    public void handleCancelOutgoingRequest() {
        ExchangeRequest selected = outgoingTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("No Selection", "Please select an outgoing request to cancel.");
            return;
        }

        if (selected.getStatus() != ExchangeRequest.RequestStatus.PENDING) {
            showError("Cannot Cancel", "Only PENDING requests can be cancelled.");
            return;
        }

        Pharmacy currentPharmacy = getActivePharmacy();
        try {
            exchangeService.cancelRequest(selected.getRequestId(), currentPharmacy.getId());
            refreshAllData();
            logMessage("CANCELLED Outgoing Request: " + selected.getRequestId());
            showInfo("Request Cancelled", "Your request was cancelled and reserved stock returned to seller.");
        } catch (Exception e) {
            showError("Cancellation Error", e.getMessage());
            logMessage("CANCEL ERROR: " + e.getMessage());
        }
    }

    @FXML
    public void handleMarkDispatched() {
        StockTransfer selected = transfersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("No Selection", "Please select a transfer to mark as dispatched.");
            return;
        }

        try {
            exchangeService.updateTransferStatus(selected.getTransferId(), StockTransfer.TransferStatus.DISPATCHED);
            refreshAllData();
            logMessage("Transfer " + selected.getTransferId() + " marked as DISPATCHED.");
        } catch (Exception e) {
            showError("Update Error", e.getMessage());
        }
    }

    @FXML
    public void handleMarkDelivered() {
        StockTransfer selected = transfersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("No Selection", "Please select a transfer to mark as delivered.");
            return;
        }

        try {
            exchangeService.updateTransferStatus(selected.getTransferId(), StockTransfer.TransferStatus.DELIVERED);
            refreshAllData();
            logMessage("Transfer " + selected.getTransferId() + " marked as DELIVERED.");
        } catch (Exception e) {
            showError("Update Error", e.getMessage());
        }
    }

    @FXML
    public void handleVerifyAndComplete() {
        StockTransfer selected = transfersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("No Selection", "Please select a transfer to verify and complete.");
            return;
        }

        try {
            exchangeService.updateTransferStatus(selected.getTransferId(), StockTransfer.TransferStatus.VERIFIED);
            refreshAllData();
            logMessage("Transfer " + selected.getTransferId() + " VERIFIED & COMPLETED. Final stock deducted.");
            showInfo("Transfer Completed", "Transfer verified successfully! Stock transfer is complete.");
        } catch (Exception e) {
            showError("Completion Error", e.getMessage());
        }
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
