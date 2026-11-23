package org.example.medistock;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/**
 * Direct Launcher for Member 3 (Stock Exchange & Requests).
 * Opens the Stock Exchange, Surplus Marketplace, and Transfer Tracking views directly
 * with the Unified JavaFX CSS Styling System applied.
 */
public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        URL fxml = getClass().getResource("/org/example/medistock/view/exchange/marketplace-view.fxml");
        if (fxml == null) {
            throw new IOException("Cannot locate marketplace-view.fxml");
        }

        Parent root = FXMLLoader.load(fxml);
        Scene scene = new Scene(root, 1180, 740);

        URL css = getClass().getResource("/com/medistock/css/main.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }

        stage.setTitle("MediStock — Stock Exchange & Surplus Marketplace");
        stage.setMinWidth(980);
        stage.setMinHeight(640);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
