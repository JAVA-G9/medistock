package com.medistock.util;

import com.medistock.AppConfig;
import com.medistock.MainApp;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/**
 * Utility to manage scene and view transitions across the MediStock application.
 * Owner: Member 1.
 */
public final class NavigationManager {

    private NavigationManager() {
    }

    /**
     * Switch the root scene of the stage that originated the given event.
     */
    public static void switchScene(Event event, String fxmlPath) {
        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            URL fxmlUrl = MainApp.class.getResource(fxmlPath);
            if (fxmlUrl == null) {
                System.err.println("Cannot find FXML: " + fxmlPath);
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            Scene scene = new Scene(root, AppConfig.WINDOW_WIDTH, AppConfig.WINDOW_HEIGHT);
            URL cssUrl = MainApp.class.getResource(AppConfig.MAIN_CSS);
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Load an FXML Parent node to be set inside a container (e.g. contentArea in Dashboard).
     */
    public static Parent loadView(String fxmlPath) throws IOException {
        URL fxmlUrl = MainApp.class.getResource(fxmlPath);
        if (fxmlUrl == null) {
            throw new IOException("Cannot locate FXML resource: " + fxmlPath);
        }
        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        return loader.load();
    }
}
