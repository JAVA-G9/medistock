
package org.example.medistock.config;

import java.io.InputStream;
import java.util.Properties;

public class DatabaseConfig {

    private static final Properties properties = new Properties();

    static {
        try {
            InputStream input = DatabaseConfig.class
                    .getResourceAsStream("/database.properties");

            if (input == null) {
                throw new RuntimeException("database.properties not found");
            }

            properties.load(input);
            input.close();

        } catch (Exception e) {
            throw new RuntimeException("Database configuration failed", e);
        }
    }

    public static String getUrl() {
        return properties.getProperty("db.url");
    }

    public static String getUsername() {
        return properties.getProperty("db.username");
    }

    public static String getPassword() {
        return properties.getProperty("db.password");
    }
}
