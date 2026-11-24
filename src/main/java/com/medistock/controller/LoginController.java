package com.medistock.controller;

import com.medistock.AppConfig;
import com.medistock.model.Pharmacy;
import com.medistock.model.User;
import com.medistock.service.PharmacyService;
import com.medistock.util.NavigationManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;

/**
 * Controller for Login and Pharmacy Registration screen.
 * Owner: Member 1.
 */
public class LoginController {

    @FXML
    private ScrollPane authScrollPane;
    @FXML
    private Label titleLabel;
    @FXML
    private Label taglineLabel;
    @FXML
    private TabPane authTabPane;

    // Login Form Fields
    @FXML
    private TextField loginUsernameField;
    @FXML
    private PasswordField loginPasswordField;
    @FXML
    private Label loginErrorLabel;

    // Registration Form Fields
    @FXML
    private TextField regPharmacyNameField;
    @FXML
    private TextField regLicenseField;
    @FXML
    private TextField regCityField;
    @FXML
    private TextField regAddressField;
    @FXML
    private TextField regPhoneField;
    @FXML
    private TextField regEmailField;
    @FXML
    private TextField regFullNameField;
    @FXML
    private TextField regUsernameField;
    @FXML
    private PasswordField regPasswordField;
    @FXML
    private TextField regRoleField;
    @FXML
    private Label regErrorLabel;

    private final PharmacyService pharmacyService = PharmacyService.getInstance();

    @FXML
    private void initialize() {
        if (titleLabel != null) {
            titleLabel.setText(AppConfig.APP_NAME);
        }
        if (taglineLabel != null) {
            taglineLabel.setText(AppConfig.APP_TAGLINE);
        }
        if (authTabPane != null && authScrollPane != null) {
            authTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
                authScrollPane.setVvalue(0.0);
            });
        }
        hideMessages();
    }

    private void hideMessages() {
        if (loginErrorLabel != null) loginErrorLabel.setVisible(false);
        if (regErrorLabel != null) regErrorLabel.setVisible(false);
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        hideMessages();
        String username = loginUsernameField.getText();
        String password = loginPasswordField.getText();

        try {
            pharmacyService.login(username, password);
            NavigationManager.switchScene(event, AppConfig.DASHBOARD_VIEW);
        } catch (PharmacyService.AuthenticationException e) {
            loginErrorLabel.setText(e.getMessage());
            loginErrorLabel.setVisible(true);
        }
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        hideMessages();

        try {
            Pharmacy pharmacy = new Pharmacy();
            pharmacy.setName(regPharmacyNameField.getText());
            pharmacy.setLicenseNumber(regLicenseField.getText());
            pharmacy.setCity(regCityField.getText());
            pharmacy.setAddress(regAddressField.getText());
            pharmacy.setPhone(regPhoneField.getText());
            pharmacy.setEmail(regEmailField.getText());

            User user = new User();
            user.setFullName(regFullNameField.getText());
            user.setUsername(regUsernameField.getText());
            user.setPassword(regPasswordField.getText());
            user.setEmail(regEmailField.getText());
            user.setRole(regRoleField.getText().isBlank() ? "Chief Pharmacist" : regRoleField.getText());

            pharmacyService.register(pharmacy, user);
            NavigationManager.switchScene(event, AppConfig.DASHBOARD_VIEW);
        } catch (PharmacyService.ValidationException e) {
            regErrorLabel.setText(e.getMessage());
            regErrorLabel.setVisible(true);
        }
    }

    // Quick demo shortcuts for presentation / evaluation testing
    @FXML
    private void handleQuickApex(ActionEvent event) {
        loginUsernameField.setText("admin");
        loginPasswordField.setText("admin123");
        handleLogin(event);
    }

    @FXML
    private void handleQuickCity(ActionEvent event) {
        loginUsernameField.setText("citypharm");
        loginPasswordField.setText("city123");
        handleLogin(event);
    }

    @FXML
    private void handleQuickGreen(ActionEvent event) {
        loginUsernameField.setText("greencross");
        loginPasswordField.setText("green123");
        handleLogin(event);
    }
}
