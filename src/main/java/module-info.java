module org.example.medistock {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    // FXML reflection access
    opens org.example.medistock to javafx.fxml;
    opens org.example.medistock.controller.exchange to javafx.fxml;
    opens org.example.medistock.controller.inventory to javafx.fxml;
    opens org.example.medistock.controller.expiry to javafx.fxml;
    opens org.example.medistock.controller.notifications to javafx.fxml;
    opens org.example.medistock.controller.reports to javafx.fxml;
    opens com.medistock to javafx.fxml;
    opens com.medistock.controller to javafx.fxml;

    // TableView PropertyValueFactory reflection access
    opens org.example.medistock.model to javafx.base;
    opens com.medistock.model to javafx.base;
    opens com.medistock.concurrency to javafx.base;

    // Exports
    exports org.example.medistock;
    exports org.example.medistock.controller.exchange;
    exports org.example.medistock.controller.inventory;
    exports org.example.medistock.controller.expiry;
    exports org.example.medistock.controller.notifications;
    exports org.example.medistock.controller.reports;
    exports org.example.medistock.model;
    exports org.example.medistock.service.exchange;
    exports org.example.medistock.service.expiry;
    exports org.example.medistock.service.notifications;
    exports org.example.medistock.service.reports;
    exports org.example.medistock.repository.exchange;
    exports org.example.medistock.repository.notification;
    exports org.example.medistock.concurrency;

    exports com.medistock;
    exports com.medistock.controller;
    exports com.medistock.model;
    exports com.medistock.service;
    exports com.medistock.repository;
    exports com.medistock.concurrency;
}
