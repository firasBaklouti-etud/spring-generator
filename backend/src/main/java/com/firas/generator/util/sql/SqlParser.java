package com.firas.generator.util.sql;

import com.firas.generator.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.*;

@Component
public class SqlParser {

    private final SqlConnectionFactory sqlConnectionFactory;

    @Autowired
    public SqlParser(SqlConnectionFactory sqlConnectionFactory) {
        this.sqlConnectionFactory = sqlConnectionFactory;
    }

    public List<Table> parseSql(String sql) throws SQLException {
        return parseSql(sql, "mysql");
    }

    public List<Table> parseSql(String sql, String dialect) throws SQLException {
        SqlConnection conn = sqlConnectionFactory.get(dialect);
        return loadMetadata(conn.getConnection(sql));
    }





    public List<Table> loadMetadata(Connection connection) throws SQLException {

        DatabaseMetaData meta = connection.getMetaData();

        String catalog = safe(() -> connection.getCatalog());
        String schema = safe(() -> connection.getSchema());

        Map<String, Table> tableMap = new LinkedHashMap<>();

        // ---------------------------------------------------------
        // 1) LOAD TABLES
        // ---------------------------------------------------------
        try (ResultSet rs = meta.getTables(catalog, fixSchema(meta, schema), "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                if (tableName == null) continue;

                Table table = new Table();
                table.setName(tableName);
                table.setClassName(toClassName(tableName));
                tableMap.put(tableName, table);
            }
        }

        // ---------------------------------------------------------
        // 2) LOAD COLUMNS
        // ---------------------------------------------------------
        for (Table table : tableMap.values()) {

            try (ResultSet rs = meta.getColumns(catalog, fixSchema(meta, schema), table.getName(), "%")) {
                while (rs.next()) {

                    Column col = new Column();

                    String colName = rs.getString("COLUMN_NAME");
                    col.setName(colName);
                    col.setFieldName(toFieldName(colName));

                    String sqlType = rs.getString("TYPE_NAME");
                    col.setType(sqlType != null ? sqlType : "");

                    col.setJavaType(mapJavaType(sqlType));

                    // Auto-increment detection
                    String autoInc = safe(() -> rs.getString("IS_AUTOINCREMENT"));
                    col.setAutoIncrement("YES".equalsIgnoreCase(autoInc));

                    int nullable = rs.getInt("NULLABLE");
                    col.setNullable(nullable != DatabaseMetaData.columnNoNulls);

                    table.addColumn(col);
                }
            }
        }

        // ---------------------------------------------------------
        // 3) PRIMARY KEYS
        // ---------------------------------------------------------
        for (Table table : tableMap.values()) {
            try (ResultSet rs = meta.getPrimaryKeys(catalog, fixSchema(meta, schema), table.getName())) {
                while (rs.next()) {
                    String pkCol = rs.getString("COLUMN_NAME");
                    for (Column col : table.getColumns()) {
                        if (col.getName().equals(pkCol)) {
                            col.setPrimaryKey(true);
                        }
                    }
                }
            }
        }

        // ---------------------------------------------------------
        // 4) FOREIGN KEYS
        // ---------------------------------------------------------
        for (Table table : tableMap.values()) {
            try (ResultSet rs = meta.getImportedKeys(catalog, fixSchema(meta, schema), table.getName())) {
                while (rs.next()) {
                    String fkCol = rs.getString("FKCOLUMN_NAME");
                    String refTable = rs.getString("PKTABLE_NAME");
                    String refCol = rs.getString("PKCOLUMN_NAME");

                    for (Column col : table.getColumns()) {
                        if (col.getName().equals(fkCol)) {
                            col.setForeignKey(true);
                            col.setReferencedTable(refTable);
                            col.setReferencedColumn(refCol);
                        }
                    }
                }
            }
        }

        // ---------------------------------------------------------
        // 5) UNIQUE INDEXES
        // ---------------------------------------------------------
        for (Table table : tableMap.values()) {

            try (ResultSet rs = meta.getIndexInfo(catalog, fixSchema(meta, schema), table.getName(), false, false)) {
                while (rs.next()) {

                    boolean nonUnique = rs.getBoolean("NON_UNIQUE");
                    if (nonUnique) continue;

                    String colName = rs.getString("COLUMN_NAME");
                    if (colName == null) continue;

                    for (Column col : table.getColumns()) {
                        if (col.getName().equals(colName)) {
                            col.setUnique(true);
                        }
                    }
                }
            } catch (Exception ignore) {}
        }

        // ---------------------------------------------------------
        // 6) RELATIONSHIPS
        // ---------------------------------------------------------
        buildRelations(tableMap);

        return new ArrayList<>(tableMap.values());
    }

    // ==================================================================================
    // RELATIONSHIP GENERATION
    // ==================================================================================
    private void buildRelations(Map<String, Table> map) {

        for (Table table : map.values()) {

            List<Column> fkCols = table.getColumns().stream()
                    .filter(Column::isForeignKey)
                    .toList();

            // ---------------------------------------------------------
            // JOIN TABLE (Many-to-Many)
            // ---------------------------------------------------------
            if (fkCols.size() >= 2 && isJoinTable(table, fkCols)) {

                table.setJoinTable(true);

                Column fk1 = fkCols.get(0);
                Column fk2 = fkCols.get(1);

                Table t1 = map.get(fk1.getReferencedTable());
                Table t2 = map.get(fk2.getReferencedTable());

                if (t1 != null && t2 != null) {

                    // t1 → t2
                    Relationship r1 = new Relationship(RelationshipType.MANY_TO_MANY,
                            t1.getName(), t2.getName());

                    r1.setJoinTable(table.getName());
                    r1.setSourceColumn(fk1.getName());
                    r1.setTargetColumn(fk2.getReferencedColumn());
                    r1.setFieldName(plural(toFieldName(t2.getName())));
                    r1.setTargetClassName(t2.getClassName());
                    t1.addRelationship(r1);

                    // t2 → t1
                    Relationship r2 = new Relationship(RelationshipType.MANY_TO_MANY,
                            t2.getName(), t1.getName());

                    r2.setJoinTable(table.getName());
                    r2.setSourceColumn(fk2.getName());
                    r2.setTargetColumn(fk1.getReferencedColumn());
                    r2.setFieldName(plural(toFieldName(t1.getName())));
                    r2.setTargetClassName(t1.getClassName());
                    t2.addRelationship(r2);
                }
                continue;
            }

            // ---------------------------------------------------------
            // NORMAL RELATIONS (ManyToOne / OneToMany / OneToOne)
            // ---------------------------------------------------------
            for (Column col : fkCols) {

                Table ref = map.get(col.getReferencedTable());
                if (ref == null) continue;

                // --- MANY TO ONE ---
                Relationship manyToOne = new Relationship(
                        RelationshipType.MANY_TO_ONE,
                        table.getName(),
                        ref.getName()
                );

                manyToOne.setSourceColumn(col.getName());                 // FK column
                manyToOne.setTargetColumn(col.getReferencedColumn());     // PK column
                manyToOne.setFieldName(toFieldName(ref.getName()));
                manyToOne.setTargetClassName(ref.getClassName());

                table.addRelationship(manyToOne);

                // --- ONE TO MANY (inverse) ---
                Relationship oneToMany = new Relationship(
                        RelationshipType.ONE_TO_MANY,
                        ref.getName(),
                        table.getName()
                );

                oneToMany.setMappedBy(manyToOne.getFieldName());
                oneToMany.setSourceColumn(col.getReferencedColumn());     // PK in ref table
                oneToMany.setTargetColumn(col.getName());                 // FK in child
                oneToMany.setFieldName(plural(toFieldName(table.getName())));
                oneToMany.setTargetClassName(table.getClassName());

                ref.addRelationship(oneToMany);

                // --- ONE TO ONE ---
                if (col.isUnique()) {
                    manyToOne.setType(RelationshipType.ONE_TO_ONE);
                    oneToMany.setType(RelationshipType.ONE_TO_ONE);
                    oneToMany.setFieldName(toFieldName(table.getName()));
                }
            }
        }
    }


    private boolean isJoinTable(Table table, List<Column> fk) {
        int total = table.getColumns().size();
        return total >= 2 && fk.size() >= Math.max(2, (int) Math.ceil(total * 0.6));
    }

    // ==================================================================================
    // HELPERS
    // ==================================================================================

    private String toClassName(String s) {
        if (s == null) return "";
        StringBuilder out = new StringBuilder();
        boolean cap = true;
        for (char c : s.toCharArray()) {
            if (c == '_' || c == '-' || c == ' ') {
                cap = true;
            } else {
                out.append(cap ? Character.toUpperCase(c) : Character.toLowerCase(c));
                cap = false;
            }
        }
        return out.toString();
    }

    private String toFieldName(String s) {
        String c = toClassName(s);
        return Character.toLowerCase(c.charAt(0)) + c.substring(1);
    }

    private String plural(String s) {
        if (s.endsWith("y"))
            return s.substring(0, s.length() - 1) + "ies";
        if (s.endsWith("s"))
            return s + "es";
        return s + "s";
    }

    private String mapJavaType(String type) {
        if (type == null) return "String";
        String t = type.toUpperCase();

        if (t.contains("CHAR") || t.contains("TEXT") || t.contains("CLOB")) return "String";
        if (t.contains("BIGINT")) return "Long";
        if (t.equals("TINYINT") || t.equals("TINYINT(1)")) return "Boolean";
        if (t.contains("INT")) return "Integer";
        if (t.contains("DECIMAL") || t.contains("NUMERIC")) return "java.math.BigDecimal";
        if (t.contains("DATE") || t.contains("TIME")) return "java.time.LocalDateTime";
        if (t.contains("BLOB") || t.contains("BINARY")) return "byte[]";

        return "String";
    }

    private String fixSchema(DatabaseMetaData meta, String schema) {
        try {
            String product = meta.getDatabaseProductName().toLowerCase();
            if (product.contains("mysql") || product.contains("mariadb"))
                return null; // MySQL ignores schema
            return schema;
        } catch (Exception e) {
            return schema;
        }
    }

    private <T> T safe(Supplier<T> s) {
        try {
            return s.get();
        } catch (Exception e) {
            return null;
        }
    }

    private interface Supplier<T> {
        T get() throws Exception;
    }
}
