module org.example.medistock {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.example.medistock to javafx.fxml;
    exports org.example.medistock;

    opens org.example.medistock.controller.inventory to javafx.fxml;
    exports org.example.medistock.controller.inventory;
    requires java.sql;
}