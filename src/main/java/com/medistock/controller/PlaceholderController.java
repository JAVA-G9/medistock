package com.medistock.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * Controller for placeholder views representing modules owned by Members 2, 3, and 4.
 * Coordinates team architectural boundaries for Evaluation 1.
 * Owner: Member 1 (shared coordination).
 */
public class PlaceholderController {

    @FXML
    private Label moduleTitleLabel;
    @FXML
    private Label moduleOwnerLabel;
    @FXML
    private Label moduleDescriptionLabel;
    @FXML
    private Label classesLabel;
    @FXML
    private Label concurrencyTopicLabel;

    public void setModuleInfo(String title, String owner, String description, String classes, String concurrencyTopic) {
        if (moduleTitleLabel != null) moduleTitleLabel.setText(title);
        if (moduleOwnerLabel != null) moduleOwnerLabel.setText(owner);
        if (moduleDescriptionLabel != null) moduleDescriptionLabel.setText(description);
        if (classesLabel != null) classesLabel.setText(classes);
        if (concurrencyTopicLabel != null) concurrencyTopicLabel.setText(concurrencyTopic);
    }
}
