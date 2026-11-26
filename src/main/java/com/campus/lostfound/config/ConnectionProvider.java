package com.campus.lostfound.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class ConnectionProvider {
    private final DatabaseConfig config;

    public ConnectionProvider(DatabaseConfig config) {
        this.config = config;
    }

    public Connection open() throws SQLException {
        return DriverManager.getConnection(config.url(), config.username(), config.password());
    }
}
