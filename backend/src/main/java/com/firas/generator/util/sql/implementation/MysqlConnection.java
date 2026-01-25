package com.firas.generator.util.sql.implementation;

import com.firas.generator.util.sql.SqlConnection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MysqlConnection implements SqlConnection {
    // Railway MySQL environment variables
    // Use public host/port for local dev, internal for Railway deployment
    private final String host = getHost();
    private final String port = getPort();
    private final String user = System.getenv("MYSQLUSER") != null ? System.getenv("MYSQLUSER") : "root";
    private final String pass = System.getenv("MYSQLPASSWORD") != null ? System.getenv("MYSQLPASSWORD") : System.getenv("MYSQL_ROOT_PASSWORD");
    private final String dbName = System.getenv("MYSQLDATABASE") != null ? System.getenv("MYSQLDATABASE") : System.getenv("MYSQL_DATABASE");

    // For local development, use public URL; on Railway, use internal
    private static String getHost() {
        // Check if running on Railway (internal host resolves)
        String internalHost = System.getenv("MYSQLHOST");
        String publicUrl = System.getenv("MYSQL_PUBLIC_URL");

        // If we have a public URL, extract host from it for local dev
        // Format: mysql://user:pass@host:port/database
        if (publicUrl != null && !isRunningOnRailway()) {
            try {
                String hostPort = publicUrl.split("@")[1].split("/")[0];
                return hostPort.split(":")[0];
            } catch (Exception e) {
                // fallback
            }
        }
        return internalHost;
    }

    private static String getPort() {
        String publicUrl = System.getenv("MYSQL_PUBLIC_URL");

        if (publicUrl != null && !isRunningOnRailway()) {
            try {
                String hostPort = publicUrl.split("@")[1].split("/")[0];
                return hostPort.split(":")[1];
            } catch (Exception e) {
                // fallback
            }
        }
        return System.getenv("MYSQLPORT") != null ? System.getenv("MYSQLPORT") : "3306";
    }

    // Check if running on Railway by looking for RAILWAY_ENVIRONMENT variable
    private static boolean isRunningOnRailway() {
        return System.getenv("RAILWAY_ENVIRONMENT") != null;
    }

    @Override
    public Connection getConnection(String sql) throws SQLException {

        // 1. Drop/Create DB using root connection (without try-with-resources on final conn)
        String adminUrl =
                "jdbc:mysql://" + host + ":" + port + "/?useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=UTC";

        try (Connection rootConn = DriverManager.getConnection(adminUrl, user, pass)) {
            rootConn.prepareStatement("DROP DATABASE IF EXISTS " + dbName).execute();
            rootConn.prepareStatement("CREATE DATABASE " + dbName).execute();
        }

        // 2. Open connection to new DB (IMPORTANT: do NOT auto-close it)
        String url =
                "jdbc:mysql://" + host + ":" + port + "/" + dbName + "?useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=UTC";

        Connection conn = DriverManager.getConnection(url, user, pass);

        // 3. Execute SQL dump
        for (String stmt : sql.split(";")) {
            String trimmed = stmt.trim();
            if (!trimmed.isEmpty()) {
                try {
                    conn.prepareStatement(trimmed).execute();
                } catch (SQLException e) {
                    System.err.println("SQL Exec error: " + trimmed);
                    e.printStackTrace();
                }
            }
        }

        // 4. Return the OPEN connection
        return conn;
    }
}
