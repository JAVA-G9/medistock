package org.example.medistock;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/**
 * Standalone Desktop Runner for Member 3 (Stock Exchange & Requests).
 * Allows the user or evaluator to run and test Member 3's module independently
 * without going through the login screen or other member modules.
 */
public class ExchangeStandaloneLauncher extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        URL fxml = getClass().getResource("/org/example/medistock/view/exchange/exchange-view.fxml");
        if (fxml == null) {
            throw new IOException("Cannot locate exchange-view.fxml");
        }

        Parent root = FXMLLoader.load(fxml);
        Scene scene = new Scene(root, 1180, 740);

        URL css = getClass().getResource("/com/medistock/css/main.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }

        stage.setTitle("MediStock — Stock Exchange & Surplus Marketplace (Member 3)");
        stage.setMinWidth(980);
        stage.setMinHeight(640);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
