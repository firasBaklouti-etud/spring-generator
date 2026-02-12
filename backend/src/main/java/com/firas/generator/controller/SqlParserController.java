package com.firas.generator.controller;

import com.firas.generator.model.Table;
import com.firas.generator.util.sql.SqlParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.List;

/**
 * REST Controller for parsing SQL schemas into table metadata.
 * 
 * This controller provides an endpoint to transform SQL CREATE TABLE and ALTER TABLE
 * statements into structured table metadata objects. These objects contain information
 * about columns, data types, constraints, and relationships that can be used for
 * code generation.
 * 
 * This is the first step in a two-phase project generation workflow:
 * 1. Parse SQL using this controller to get table metadata
 * 2. Pass the table metadata to GeneratorController to generate the project
 * 
 * @author Firas Baklouti
 * @version 1.0
 * @since 2025-12-01
 */
@Slf4j
@RestController
@RequestMapping("/api/sqlParser")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SqlParserController {

    /**
     * SQL parser utility for extracting table metadata from SQL statements
     */
    private final SqlParser sqlParser;

    /**
     * Parses SQL statements and returns structured table metadata.
     * 
     * This endpoint accepts SQL CREATE TABLE and ALTER TABLE statements and transforms
     * them into a list of Table objects containing detailed metadata about columns,
     * constraints, relationships, and data types. The resulting table metadata can be
     * used as input for the project generation endpoint.
     * 
     * Supported SQL statements:
     * - CREATE TABLE with inline constraints
     * - ALTER TABLE ADD PRIMARY KEY
     * - ALTER TABLE ADD FOREIGN KEY
     * - ALTER TABLE MODIFY COLUMN
     * - ALTER TABLE DROP COLUMN
     * - ALTER TABLE ADD UNIQUE
     * 
     * @param sql The SQL statements to parse (as a path variable)
     * @return List of Table objects containing parsed metadata
     * @throws SQLException If the SQL syntax is invalid or cannot be parsed
     */
    @GetMapping("/{sql}")
    public List<Table> parseSql(
            @PathVariable String sql,
            @RequestParam(required = false, defaultValue = "mysql") String dialect
    ) throws SQLException {
        log.debug("Parsing SQL with dialect: {}", dialect);
        return sqlParser.parseSql(sql, dialect);
    }
}
