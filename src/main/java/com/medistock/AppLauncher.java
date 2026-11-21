package com.medistock;

import javafx.application.Application;

/**
 * Plain entry point (no Application superclass) so the app also starts
 * from a fat JAR / IDE without JavaFX module-path errors.
 * Owner: Member 1.
 */
public class AppLauncher {
    public static void main(String[] args) {
        Application.launch(MainApp.class, args);
    }
}
