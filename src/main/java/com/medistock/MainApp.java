package com.medistock;

import com.medistock.concurrency.ThreadManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/**
 * Application startup.
 * Owner: Member 1 (Application startup, navigation, and shutdown coordination).
 */
public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        URL fxml = MainApp.class.getResource(AppConfig.LOGIN_VIEW);
        if (fxml == null) {
            throw new IOException("Cannot find initial view: " + AppConfig.LOGIN_VIEW);
        }
        Parent root = new FXMLLoader(fxml).load();

        Scene scene = new Scene(root, AppConfig.WINDOW_WIDTH, AppConfig.WINDOW_HEIGHT);
        URL css = MainApp.class.getResource(AppConfig.MAIN_CSS);
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }

        stage.setTitle(AppConfig.APP_NAME + " — " + AppConfig.APP_TAGLINE);
        stage.setMinWidth(AppConfig.MIN_WIDTH);
        stage.setMinHeight(AppConfig.MIN_HEIGHT);
        stage.setScene(scene);

        // Multithreading lifecycle requirement: clean shutdown of background threads
        stage.setOnCloseRequest(e -> {
            ThreadManager.getInstance().shutdownAll();
        });

        stage.show();
    }

    @Override
    public void stop() {
        ThreadManager.getInstance().shutdownAll();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
