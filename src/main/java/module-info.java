module com.medistock {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.medistock to javafx.fxml;
    opens com.medistock.controller to javafx.fxml;
    opens com.medistock.model to javafx.base;
    opens com.medistock.concurrency to javafx.base;

    exports com.medistock;
    exports com.medistock.controller;
    exports com.medistock.model;
    exports com.medistock.service;
    exports com.medistock.repository;
    exports com.medistock.repository.memory;
    exports com.medistock.concurrency;
    exports com.medistock.util;
}
