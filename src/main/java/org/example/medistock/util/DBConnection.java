
package org.example.medistock.util;

import org.example.medistock.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    public static Connection connectToMySql() {
        Connection con = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            DriverManager.setLoginTimeout(10); // 10 second timeout

            String url = DatabaseConfig.getUrl();
            if (url != null && !url.contains("connectTimeout")) {
                url += (url.contains("?") ? "&" : "?") + "connectTimeout=10000&socketTimeout=10000";
            }

            con = DriverManager.getConnection(
                    url,
                    DatabaseConfig.getUsername(),
                    DatabaseConfig.getPassword()
            );

            System.out.println("[DBConnection] Database Connected Successfully");
        }
        catch (Throwable e) {
            System.err.println("[DBConnection] Database connection error: " + e.getMessage());
        }

        return con;
    }
}
