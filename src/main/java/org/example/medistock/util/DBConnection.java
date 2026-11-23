
package org.example.medistock.util;

import javafx.application.Platform;
import org.example.medistock.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static volatile Boolean dbReachable = null;
    private static long lastCheckTime = 0;
    private static final long RETRY_INTERVAL_MS = 60_000; // Only retry remote DB after 60 seconds if it failed

    public static Connection connectToMySql() {
        // Strict UI responsiveness rule: Never block the JavaFX Application Thread on network I/O
        if (Platform.isFxApplicationThread()) {
            return null;
        }

        long now = System.currentTimeMillis();

        // Circuit breaker: If remote DB was unreachable, fail fast without hanging the JavaFX UI
        if (Boolean.FALSE.equals(dbReachable) && (now - lastCheckTime < RETRY_INTERVAL_MS)) {
            return null;
        }

        Connection con = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            DriverManager.setLoginTimeout(2); // 2 second timeout max

            String url = DatabaseConfig.getUrl();
            if (url != null && !url.contains("connectTimeout")) {
                url += (url.contains("?") ? "&" : "?") + "connectTimeout=2000&socketTimeout=2000";
            }

            con = DriverManager.getConnection(
                    url,
                    DatabaseConfig.getUsername(),
                    DatabaseConfig.getPassword()
            );

            dbReachable = true;
            lastCheckTime = now;
            System.out.println("[DBConnection] Database Connected Successfully");
        }
        catch (Throwable e) {
            dbReachable = false;
            lastCheckTime = now;
            System.err.println("[DBConnection] Cloud DB unreachable (offline/firewall). Operating in seamless IN-MEMORY mode: " + e.getMessage());
        }

        return con;
    }

    public static boolean isAvailable() {
        return Boolean.TRUE.equals(dbReachable);
    }

    public static void resetConnectionCheck() {
        dbReachable = null;
        lastCheckTime = 0;
    }
}
