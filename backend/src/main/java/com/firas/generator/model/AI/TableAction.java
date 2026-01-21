package com.firas.generator.model.AI;

import com.firas.generator.model.Table;

import java.util.List;

public class TableAction {
    private TableActionType type;
    private List<Table> tables;
    private List<String> tableNames;
    private List<Table> newSchema;

    public TableAction(TableActionType type, List<Table> tables, List<String> tableNames, List<Table> newSchema) {
        this.type = type;
        this.tables = tables;
        this.tableNames = tableNames;
        this.newSchema = newSchema;
    }

    public TableAction() {
    }

    public TableActionType getType() {
        return type;
    }

    public void setType(TableActionType type) {
        this.type = type;
    }

    public List<Table> getTables() {
        return tables;
    }

    public void setTables(List<Table> tables) {
        this.tables = tables;
    }

    public List<String> getTableNames() {
        return tableNames;
    }

    public void setTableNames(List<String> tableNames) {
        this.tableNames = tableNames;
    }

    public List<Table> getNewSchema() {
        return newSchema;
    }

    public void setNewSchema(List<Table> newSchema) {
        this.newSchema = newSchema;
    }

    @Override
    public String toString() {
        return "TableAction{" +
                "type=" + type +
                ", tables=" + tables +
                ", tableNames=" + tableNames +
                ", newSchema=" + newSchema +
                '}';
    }
}
