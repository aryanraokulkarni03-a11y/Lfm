package com.campus.lostfound.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public record DatabaseConfig(String url, String username, String password) {
    public static DatabaseConfig load() {
        var properties = new Properties();
        try (InputStream stream = DatabaseConfig.class.getResourceAsStream("/application.properties")) {
            if (stream != null) properties.load(stream);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not load database configuration", ex);
        }
        return new DatabaseConfig(
                value("CAMPUS_DB_URL", properties, "db.url", "jdbc:mysql://localhost:3306/campus_lost_found"),
                value("CAMPUS_DB_USERNAME", properties, "db.username", "campus_app"),
                value("CAMPUS_DB_PASSWORD", properties, "db.password", "")
        );
    }

    private static String value(String environmentName, Properties properties, String propertyName, String fallback) {
        String environmentValue = System.getenv(environmentName);
        return environmentValue == null || environmentValue.isBlank()
                ? properties.getProperty(propertyName, fallback)
                : environmentValue;
    }
}
