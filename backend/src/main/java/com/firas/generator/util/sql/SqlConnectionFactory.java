package com.firas.generator.util.sql;

import com.firas.generator.util.sql.implementation.MysqlConnection;
import com.firas.generator.util.sql.implementation.PostgresqlConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SqlConnectionFactory {

    private final MysqlConnection mysqlConnection;
    private final PostgresqlConnection postgresqlConnection;

    @Autowired
    public SqlConnectionFactory(MysqlConnection mysqlConnection, PostgresqlConnection postgresqlConnection) {
        this.mysqlConnection = mysqlConnection;
        this.postgresqlConnection = postgresqlConnection;
    }

    public SqlConnection get(String dialect) {
        switch (dialect.toLowerCase()) {
            case "postgresql":
                return postgresqlConnection;
            case "mysql":
            default:
                return mysqlConnection;
        }
    }
}