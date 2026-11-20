module org.example.medistock {

    requires javafx.controls;
    requires javafx.fxml;

    opens org.example.medistock to javafx.fxml;
    opens org.example.medistock.controller to javafx.fxml;
    opens org.example.medistock.model to javafx.base;

    exports org.example.medistock;
}