module org.example.medistock {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.example.medistock to javafx.fxml;
    exports org.example.medistock;
}