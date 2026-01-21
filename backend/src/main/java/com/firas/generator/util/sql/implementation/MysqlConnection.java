package com.firas.generator.util.sql.implementation;

import com.firas.generator.util.sql.SqlConnection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MysqlConnection implements SqlConnection {
    private final String host = System.getenv("MYSQL_DB_HOST");
    private final String user = System.getenv("MYSQL_DB_USER");
    private final String pass = System.getenv("MYSQL_DB_PASSWORD");
    private final String dbName = System.getenv("MYSQL_DB_NAME");

    @Override
    public Connection getConnection(String sql) throws SQLException {

        // 1. Drop/Create DB using root connection (without try-with-resources on final conn)
        String adminUrl =
                "jdbc:mysql://" + host + "/?useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=UTC";

        try (Connection rootConn = DriverManager.getConnection(adminUrl, user, pass)) {
            rootConn.prepareStatement("DROP DATABASE IF EXISTS " + dbName).execute();
            rootConn.prepareStatement("CREATE DATABASE " + dbName).execute();
        }

        // 2. Open connection to new DB (IMPORTANT: do NOT auto-close it)
        String url =
                "jdbc:mysql://" + host + "/" + dbName + "?useSSL=false&serverTimezone=UTC";

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
