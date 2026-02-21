package com.firas.generator.mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.firas.generator.mcp.McpToolProvider;
import com.firas.generator.model.Table;
import com.firas.generator.util.sql.SqlParser;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * MCP tool that parses SQL DDL statements into structured table metadata.
 */
@Component
public class ParseSqlTool implements McpToolProvider {

    private static final Logger log = LoggerFactory.getLogger(ParseSqlTool.class);

    private final SqlParser sqlParser;
    private final ObjectMapper objectMapper;

    public ParseSqlTool(SqlParser sqlParser, ObjectMapper objectMapper) {
        this.sqlParser = sqlParser;
        this.objectMapper = objectMapper;
    }

    @Override
    public McpServerFeatures.SyncToolSpecification getToolSpecification() {
        McpSchema.JsonSchema inputSchema = new McpSchema.JsonSchema(
                "object",
                Map.of(
                        "sql", Map.of("type", "string", "description", "SQL CREATE TABLE DDL statements to parse"),
                        "dialect", Map.of("type", "string", "enum", List.of("mysql", "postgresql"), "description", "SQL dialect (default: mysql)")
                ),
                List.of("sql"),
                null, null, null
        );

        McpSchema.Tool tool = McpSchema.Tool.builder()
                .name("parse_sql")
                .description("Parse SQL CREATE TABLE DDL statements into structured table metadata. " +
                        "Returns tables with columns, types, primary keys, foreign keys, and detected relationships. " +
                        "Use this as the first step to generate a project from a database schema.")
                .inputSchema(inputSchema)
                .build();

        return new McpServerFeatures.SyncToolSpecification(tool, (exchange, arguments) -> {
            try {
                String sql = (String) arguments.get("sql");
                String dialect = arguments.containsKey("dialect")
                        ? (String) arguments.get("dialect")
                        : "mysql";

                log.debug("Parsing SQL with dialect: {}", dialect);
                List<Table> tables = sqlParser.parseSql(sql, dialect);
                String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(tables);

                return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(json)), false);
            } catch (Exception e) {
                log.error("Failed to parse SQL", e);
                return new McpSchema.CallToolResult(
                        List.of(new McpSchema.TextContent("Error parsing SQL: " + e.getMessage())), true);
            }
        });
    }
}
