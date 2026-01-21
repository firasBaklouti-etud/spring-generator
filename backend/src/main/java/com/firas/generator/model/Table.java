package com.firas.generator.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a database table with its columns and relationships.
 *
 * This class models a database table parsed from SQL CREATE TABLE statements.
 * It contains the table's columns, relationships to other tables, and metadata
 * used to generate JPA entity classes.
 *
 * @author Firas Baklouti
 * @version 1.0
 * @since 2025-12-01
 */
public class Table {
    private String name;
    private String className;
    private List<Column> columns = new ArrayList<>();
    private List<Relationship> relationships = new ArrayList<>();
    private boolean isJoinTable = false;
    private Map<String, Object> metadata = new HashMap<>();

    public Table() {
    }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    public void addMetadata(String key, Object value) { this.metadata.put(key, value); }

    public void addColumn(Column column) {
        this.columns.add(column);
    }

    public void addRelationship(Relationship relationship) {
        this.relationships.add(relationship);
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public List<Column> getColumns() { return columns; }
    public void setColumns(List<Column> columns) { this.columns = columns; }

    public List<Relationship> getRelationships() { return relationships; }
    public void setRelationships(List<Relationship> relationships) { this.relationships = relationships; }

    public boolean isJoinTable() { return isJoinTable; }
    public void setJoinTable(boolean joinTable) { isJoinTable = joinTable; }

    @Override
    public String toString() {
        return "Table{" +
                "name='" + name + '\'' +
                ", className='" + className + '\'' +
                ", columns=" + columns +
                ", relationships=" + relationships +
                ", isJoinTable=" + isJoinTable +
                '}';
    }
}
