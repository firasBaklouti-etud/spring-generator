package com.firas.generator.util.sql;

import java.sql.Connection;
import java.sql.SQLException;

public interface SqlConnection {
    public Connection getConnection(String sql) throws SQLException;
}
