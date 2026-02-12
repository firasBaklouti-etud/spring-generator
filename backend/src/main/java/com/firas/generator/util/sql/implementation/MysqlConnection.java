package com.firas.generator.util.sql.implementation;

import com.firas.generator.util.sql.SqlConnection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Component
public class MysqlConnection implements SqlConnection {

    @Value("${mysql.host}")
    private String host;

    @Value("${mysql.port}")
    private String port;

    @Value("${mysql.user}")
    private String user;

    @Value("${mysql.password}")
    private String pass;

    @Value("${mysql.database}")
    private String dbName;

    @Override
    public Connection getConnection(String sql) throws SQLException {
        // Debug logging
        System.out.println("MySQL Connection Config:");
        System.out.println("  Host: " + this.host);
        System.out.println("  Port: " + this.port);
        System.out.println("  User: " + this.user);
        System.out.println("  Database: " + this.dbName);

        // 1. Drop/Create DB using root connection
        String adminUrl =
                "jdbc:mysql://" + host + ":" + port + "/?useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=UTC";

        try (Connection rootConn = DriverManager.getConnection(adminUrl, user, pass)) {
            rootConn.prepareStatement("DROP DATABASE IF EXISTS " + dbName).execute();
            rootConn.prepareStatement("CREATE DATABASE " + dbName).execute();
        }

        // 2. Open connection to new DB
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
