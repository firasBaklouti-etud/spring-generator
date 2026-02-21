package com.firas.generator.util;

import com.firas.generator.model.Column;
import com.firas.generator.model.Table;
import com.firas.generator.util.sql.SqlParser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.nio.file.*;
import java.util.*;

import static junit.framework.TestCase.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SqlParserTest {

    @Autowired
    private SqlParser sqlParser;

    @Test
    public void testSqlParserWithHardSchema() throws Exception {
        // Load the hard schema SQL (place the file at src/test/resources/hard_schema.sql)
        String sql = Files.readString(Paths.get("src/test/resources/hard_schema.sql"));

        List<Table> tables = sqlParser.parseSql(sql);

        // Basic sanity checks
        assertNotNull("Tables should not be null", tables);
        // Expecting the tables: users, roles, user_roles, products, orders, order_items => 6
        assertEquals("Should parse 6 tables", 6, tables.size());

        // Find user_roles join table
        Optional<Table> userRolesOpt = tables.stream()
                .filter(t -> "user_roles".equalsIgnoreCase(t.getName()))
                .findFirst();

        assertTrue("user_roles table must exist", userRolesOpt.isPresent());
        Table userRoles = userRolesOpt.get();

        // join table flag must be true
        assertTrue("user_roles should be detected as join table", userRoles.isJoinTable());

        // user_roles should have exactly 2 columns that are FKs and 2 PKs (composite PK)
        long fkCount = userRoles.getColumns().stream().filter(Column::isForeignKey).count();
        long pkCount = userRoles.getColumns().stream().filter(Column::isPrimaryKey).count();

        assertEquals("user_roles should have 2 foreign keys", 2, fkCount);
        assertEquals("user_roles should have 2 primary key columns (composite PK)", 2, pkCount);

        // Ensure orders and order_items relationship presence and FK detection
        Optional<Table> orderItemsOpt = tables.stream()
                .filter(t -> "order_items".equalsIgnoreCase(t.getName()))
                .findFirst();
        assertTrue("order_items table must exist", orderItemsOpt.isPresent());
        Table orderItems = orderItemsOpt.get();

        long oiFk = orderItems.getColumns().stream().filter(Column::isForeignKey).count();
        assertTrue("order_items should have at least 2 foreign keys", oiFk >= 2);

        // Ensure products table exists and has relationships (many-to-one or many-to-many)
        Optional<Table> productsOpt = tables.stream()
                .filter(t -> "products".equalsIgnoreCase(t.getName()))
                .findFirst();
        assertTrue("products table must exist", productsOpt.isPresent());
        Table products = productsOpt.get();

        assertFalse("products should have at least one relationship", products.getRelationships().isEmpty());

        // Ensure users table has inverse relationships (one-to-many) because orders reference users
        Optional<Table> usersOpt = tables.stream()
                .filter(t -> "users".equalsIgnoreCase(t.getName()))
                .findFirst();
        assertTrue("users table must exist", usersOpt.isPresent());
        Table users = usersOpt.get();

        assertFalse("users should have relationships (inverse of orders)", users.getRelationships().isEmpty());

        // Sanity: total relationships across all tables should be more than a handful
        long totalRelationships = tables.stream().mapToLong(t -> t.getRelationships().size()).sum();
        assertTrue("Expect many relationships (stress test)", totalRelationships > 6);

        // Optional: print for manual inspection
        System.out.println("=== JDBC PARSED TABLES (HARD SCHEMA) ===");
        tables.forEach(t -> {
            System.out.println("\nTABLE: " + t.getName());
            System.out.println("  isJoinTable: " + t.isJoinTable());
            System.out.println("  columns:");
            t.getColumns().forEach(c -> System.out.println("    - " + c.getName()
                    + " (pk=" + c.isPrimaryKey()
                    + ", fk=" + c.isForeignKey()
                    + ", unique=" + c.isUnique()
                    + ", refTable=" + c.getReferencedTable()
                    + ", refCol=" + c.getReferencedColumn()
                    + ")"));
            System.out.println("  relationships:");
            t.getRelationships().forEach(r -> System.out.println("    - " + r));
        });
    }

}
