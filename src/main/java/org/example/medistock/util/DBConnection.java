
package org.example.medistock.util;

import org.example.medistock.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    public static Connection connectToMySql() {

        Connection con = null;

        try {
            con = DriverManager.getConnection(
                    DatabaseConfig.getUrl(),
                    DatabaseConfig.getUsername(),
                    DatabaseConfig.getPassword()
            );
            System.out.println("Database Connected Successfully");
        }
        catch (SQLException e) {
            e.printStackTrace();
        }

        return con;
    }
}
