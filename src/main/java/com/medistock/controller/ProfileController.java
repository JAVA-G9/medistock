package com.medistock.controller;

import com.medistock.model.Pharmacy;
import com.medistock.model.User;
import com.medistock.service.PharmacyService;
import com.medistock.util.SessionContext;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Controller for viewing and editing Pharmacy Profile and Pharmacist account details.
 * Owner: Member 1.
 */
public class ProfileController {

    @FXML
    private TextField pharmacyNameField;
    @FXML
    private TextField licenseNumberField;
    @FXML
    private TextField cityField;
    @FXML
    private TextField addressField;
    @FXML
    private TextField phoneField;
    @FXML
    private TextField emailField;

    @FXML
    private TextField fullNameField;
    @FXML
    private TextField usernameField;
    @FXML
    private TextField roleField;
    @FXML
    private PasswordField passwordField;

    @FXML
    private Label statusMessageLabel;
    @FXML
    private Label pharmacyIdBadge;

    private final PharmacyService pharmacyService = PharmacyService.getInstance();

    @FXML
    private void initialize() {
        loadCurrentProfile();
    }

    public void loadCurrentProfile() {
        Pharmacy pharmacy = SessionContext.getCurrentPharmacy();
        User user = SessionContext.getCurrentUser();

        if (pharmacy != null) {
            pharmacyNameField.setText(pharmacy.getName());
            licenseNumberField.setText(pharmacy.getLicenseNumber());
            cityField.setText(pharmacy.getCity());
            addressField.setText(pharmacy.getAddress());
            phoneField.setText(pharmacy.getPhone());
            emailField.setText(pharmacy.getEmail());
            if (pharmacyIdBadge != null) {
                pharmacyIdBadge.setText("ID: " + pharmacy.getId() + " | License: " + pharmacy.getLicenseNumber());
            }
        }

        if (user != null) {
            fullNameField.setText(user.getFullName());
            usernameField.setText(user.getUsername());
            roleField.setText(user.getRole());
            passwordField.setText(user.getPassword());
        }

        if (statusMessageLabel != null) {
            statusMessageLabel.setVisible(false);
        }
    }

    @FXML
    private void handleSaveProfile(ActionEvent event) {
        Pharmacy pharmacy = SessionContext.getCurrentPharmacy();
        User user = SessionContext.getCurrentUser();

        if (pharmacy != null) {
            pharmacy.setName(pharmacyNameField.getText().trim());
            pharmacy.setLicenseNumber(licenseNumberField.getText().trim());
            pharmacy.setCity(cityField.getText().trim());
            pharmacy.setAddress(addressField.getText().trim());
            pharmacy.setPhone(phoneField.getText().trim());
            pharmacy.setEmail(emailField.getText().trim());
            pharmacyService.updatePharmacy(pharmacy);
        }

        if (user != null) {
            user.setFullName(fullNameField.getText().trim());
            user.setRole(roleField.getText().trim());
            if (!passwordField.getText().isBlank()) {
                user.setPassword(passwordField.getText());
            }
            pharmacyService.updateUser(user);
        }

        if (statusMessageLabel != null) {
            statusMessageLabel.setText("Profile updated successfully!");
            statusMessageLabel.setStyle("-fx-text-fill: #059669; -fx-font-weight: bold;");
            statusMessageLabel.setVisible(true);
        }
    }

    @FXML
    private void handleReset(ActionEvent event) {
        loadCurrentProfile();
    }
}
