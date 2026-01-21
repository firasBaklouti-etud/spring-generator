package com.firas.generator.model;

/**
 * Represents a database column with its metadata and properties.
 * 
 * This class models a database column parsed from SQL CREATE TABLE or ALTER TABLE statements.
 * It contains information about the column's name, type, constraints, and relationships
 * to other tables. This metadata is used to generate JPA entity fields and annotations.
 * 
 * @author Firas Baklouti
 * @version 1.0
 * @since 2025-12-01
 */
public class Column {
    /** The database column name (e.g., "user_id") */
    private String name;
    
    /** The SQL data type (e.g., "VARCHAR", "INT", "BIGINT") */
    private String type;
    
    /** The Java field name in camelCase (e.g., "userId") */
    private String fieldName;
    
    /** The corresponding Java type (e.g., "String", "Integer", "Long") */
    private String javaType;
    
    /** Indicates if this column is part of the primary key */
    private boolean primaryKey;
    
    /** Indicates if this column has auto-increment enabled */
    private boolean autoIncrement;
    
    /** Indicates if this column allows NULL values */
    private boolean nullable;
    
    /** Indicates if this column is a foreign key referencing another table */
    private boolean foreignKey;
    
    /** The name of the referenced table (if this is a foreign key) */
    private String referencedTable;
    
    /** The name of the referenced column (if this is a foreign key) */
    private String referencedColumn;
    
    /** Indicates if this column has a UNIQUE constraint */
    private boolean unique;

    public Column() {
    }
    // Getters and Setters with inline documentation
    
    /** @return The database column name */
    public String getName() { return name; }
    
    /** @param name The database column name to set */
    public void setName(String name) { this.name = name; }

    /** @return The SQL data type */
    public String getType() { return type; }
    
    /** @param type The SQL data type to set */
    public void setType(String type) { this.type = type; }

    /** @return The Java field name in camelCase */
    public String getFieldName() { return fieldName; }
    
    /** @param fieldName The Java field name to set */
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }

    /** @return The corresponding Java type */
    public String getJavaType() { return javaType; }
    
    /** @param javaType The Java type to set */
    public void setJavaType(String javaType) { this.javaType = javaType; }

    /** @return true if this column is part of the primary key */
    public boolean isPrimaryKey() { return primaryKey; }
    
    /** @param primaryKey Set to true if this is a primary key column */
    public void setPrimaryKey(boolean primaryKey) { this.primaryKey = primaryKey; }

    /** @return true if this column has auto-increment enabled */
    public boolean isAutoIncrement() { return autoIncrement; }
    
    /** @param autoIncrement Set to true if auto-increment is enabled */
    public void setAutoIncrement(boolean autoIncrement) { this.autoIncrement = autoIncrement; }

    /** @return true if this column allows NULL values */
    public boolean isNullable() { return nullable; }
    
    /** @param nullable Set to true if NULL values are allowed */
    public void setNullable(boolean nullable) { this.nullable = nullable; }

    /** @return true if this column is a foreign key */
    public boolean isForeignKey() { return foreignKey; }
    
    /** @param foreignKey Set to true if this is a foreign key column */
    public void setForeignKey(boolean foreignKey) { this.foreignKey = foreignKey; }

    /** @return The name of the referenced table (if foreign key) */
    public String getReferencedTable() { return referencedTable; }
    
    /** @param referencedTable The referenced table name to set */
    public void setReferencedTable(String referencedTable) { this.referencedTable = referencedTable; }

    /** @return The name of the referenced column (if foreign key) */
    public String getReferencedColumn() { return referencedColumn; }
    
    /** @param referencedColumn The referenced column name to set */
    public void setReferencedColumn(String referencedColumn) { this.referencedColumn = referencedColumn; }

    /** @return true if this column has a UNIQUE constraint */
    public boolean isUnique() { return unique; }
    
    /** @param unique Set to true if this column has a UNIQUE constraint */
    public void setUnique(boolean unique) { this.unique = unique; }

    @Override
    public String toString() {
        return "Column{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", fieldName='" + fieldName + '\'' +
                ", javaType='" + javaType + '\'' +
                ", primaryKey=" + primaryKey +
                ", autoIncrement=" + autoIncrement +
                ", nullable=" + nullable +
                ", foreignKey=" + foreignKey +
                ", referencedTable='" + referencedTable + '\'' +
                ", referencedColumn='" + referencedColumn + '\'' +
                ", unique=" + unique +
                '}';
    }
}
