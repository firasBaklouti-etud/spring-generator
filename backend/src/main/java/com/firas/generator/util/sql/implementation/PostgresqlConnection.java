package com.firas.generator.util.sql.implementation;

import com.firas.generator.util.sql.SqlConnection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

@Component
public class PostgresqlConnection implements SqlConnection {

    @Value("${postgresql.database}")
    private String dbName;

    @Value("${postgresql.user}")
    private String user;

    @Value("${postgresql.password}")
    private String pass;

    @Value("${postgresql.host}")
    private String host;

    @Override
    public Connection getConnection(String sql) throws SQLException {
        System.out.println("=== PostgreSQL Parser ===");
        System.out.println("Host: " + host);
        System.out.println("DB: " + dbName);
        System.out.println("User: " + user);

        // --- 1. ADMIN CONNECTION (OK TO CLOSE) ---
        String adminUrl = "jdbc:postgresql://" + host + "/postgres?sslmode=require";
        System.out.println("Admin URL: " + adminUrl);

        try (Connection adminConn = DriverManager.getConnection(adminUrl, user, pass);
             Statement stmt = adminConn.createStatement()) {

            // Kill existing connections
            try {
                stmt.execute("""
                    SELECT pg_terminate_backend(pg_stat_activity.pid)
                    FROM pg_stat_activity
                    WHERE pg_stat_activity.datname = '%s'
                    AND pid <> pg_backend_pid()
                    """.formatted(dbName)
                );
            } catch (SQLException e) {
                System.err.println("Warning terminating connections: " + e.getMessage());
            }

            // Drop DB
            try {
                stmt.execute("DROP DATABASE IF EXISTS " + dbName);
                System.out.println("Dropped database: " + dbName);
            } catch (SQLException e) {
                System.err.println("Could not drop database: " + e.getMessage());
            }

            // Create DB
            try {
                stmt.execute("CREATE DATABASE " + dbName);
                System.out.println("Created database: " + dbName);
            } catch (SQLException e) {
                System.err.println("Could not create database: " + e.getMessage());
            }
        }

        // --- 2. OPEN FINAL CONNECTION (MUST NOT BE CLOSED!) ---
        String url = "jdbc:postgresql://" + host + "/" + dbName + "?sslmode=require";
        System.out.println("Target URL: " + url);

        Connection conn = DriverManager.getConnection(url, user, pass);

        // --- 3. EXECUTE ALL SQL STATEMENTS ---
        for (String stmt : sql.split(";")) {
            String trimmed = stmt.trim();
            if (!trimmed.isEmpty()) {
                try {
                    System.out.println("Executing PostgreSQL: " + (trimmed.length() > 80 ? trimmed.substring(0, 80) + "..." : trimmed));
                    conn.prepareStatement(trimmed).execute();
                } catch (SQLException e) {
                    System.err.println("Failed to execute: " + trimmed);
                    e.printStackTrace();
                }
            }
        }

        // --- 4. RETURN AN OPEN CONNECTION ---
        return conn;
    }
}
