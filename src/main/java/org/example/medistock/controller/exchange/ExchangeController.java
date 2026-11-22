package org.example.medistock.controller.exchange;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import org.example.medistock.concurrency.ExchangeWorker;
import org.example.medistock.service.exchange.ExchangeService;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Main Controller for the Stock Exchange module.
 * Folder: controller/exchange/
 * Owned by Member 3 (Stock Exchange & Requests).
 * Manages the tabbed view between Marketplace Surplus Listings and Exchange Requests/Transfers.
 */
public class ExchangeController implements Initializable {

    @FXML private TabPane exchangeTabPane;
    @FXML private Tab tabMarketplace;
    @FXML private Tab tabRequests;

    @FXML private MarketplaceController marketplaceViewController;
    @FXML private RequestsController requestsViewController;

    private ExchangeWorker exchangeWorker;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Start background daemon thread to monitor and trace transfers
        exchangeWorker = new ExchangeWorker(ExchangeService.getInstance(), 5000);
        exchangeWorker.start();

        // Refresh views whenever tabs are switched
        if (exchangeTabPane != null) {
            exchangeTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
                if (newTab == tabMarketplace && marketplaceViewController != null) {
                    marketplaceViewController.loadListings();
                } else if (newTab == tabRequests && requestsViewController != null) {
                    requestsViewController.refreshAllData();
                }
            });
        }
    }

    public void selectMarketplaceTab() {
        if (exchangeTabPane != null && tabMarketplace != null) {
            exchangeTabPane.getSelectionModel().select(tabMarketplace);
        }
    }

    public void selectRequestsTab() {
        if (exchangeTabPane != null && tabRequests != null) {
            exchangeTabPane.getSelectionModel().select(tabRequests);
        }
    }

    public void shutdown() {
        if (exchangeWorker != null) {
            exchangeWorker.stop();
        }
    }
}
