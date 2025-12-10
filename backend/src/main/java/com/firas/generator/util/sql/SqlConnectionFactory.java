package com.firas.generator.util.sql;

import com.firas.generator.util.sql.implementation.MysqlConnection;
import com.firas.generator.util.sql.implementation.PostgresqlConnection;

public class SqlConnectionFactory {
    public static SqlConnection get(String dialect) {
        switch (dialect.toLowerCase()) {
            case "postgresql":
                return new PostgresqlConnection();
            case "mysql":
            default:
                return new MysqlConnection();
        }
    }
}