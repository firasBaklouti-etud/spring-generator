package com.firas.generator.mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.firas.generator.mcp.McpToolProvider;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP tool that returns a SecurityConfig template with documentation.
 * Helps AI clients construct the securityConfig object for project generation.
 */
@Component
public class ConfigureSecurityTool implements McpToolProvider {

    private final ObjectMapper objectMapper;

    public ConfigureSecurityTool(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public McpServerFeatures.SyncToolSpecification getToolSpecification() {
        McpSchema.JsonSchema inputSchema = new McpSchema.JsonSchema(
                "object",
                Map.of(
                        "authenticationType", Map.of("type", "string",
                                "enum", List.of("BASIC", "JWT", "OAUTH2"),
                                "description", "Authentication method. JWT is recommended for REST APIs. Default: JWT"),
                        "rbacMode", Map.of("type", "string",
                                "enum", List.of("STATIC", "DYNAMIC"),
                                "description", "STATIC: Compile-time enum-based roles/permissions. DYNAMIC: Database-stored roles with runtime permission management. Default: STATIC"),
                        "principalEntity", Map.of("type", "string",
                                "description", "Name of the entity representing authenticated users (must match a table name). Default: User"),
                        "usernameField", Map.of("type", "string",
                                "description", "Field on the principal entity used as the username for authentication. Default: email"),
                        "passwordField", Map.of("type", "string",
                                "description", "Field on the principal entity used for password storage. Default: password")
                ),
                List.of(),
                null, null, null
        );

        McpSchema.Tool tool = McpSchema.Tool.builder()
                .name("configure_security")
                .description("Generate a SecurityConfig object for use in project generation requests. " +
                        "Returns a complete securityConfig JSON that can be embedded in a projectRequest. " +
                        "Supports BASIC auth, JWT (with auto-generated filter/util/controller), and OAUTH2. " +
                        "RBAC modes: STATIC (enum-based Permission/Role) or DYNAMIC (JPA Role entity with DB-stored permissions).")
                .inputSchema(inputSchema)
                .build();

        return new McpServerFeatures.SyncToolSpecification(tool,
                (exchange, arguments) -> {
                    try {
                        String authType = arguments.containsKey("authenticationType")
                                ? (String) arguments.get("authenticationType")
                                : "JWT";
                        String rbacMode = arguments.containsKey("rbacMode")
                                ? (String) arguments.get("rbacMode")
                                : "STATIC";
                        String principalEntity = arguments.containsKey("principalEntity")
                                ? (String) arguments.get("principalEntity")
                                : "User";
                        String usernameField = arguments.containsKey("usernameField")
                                ? (String) arguments.get("usernameField")
                                : "email";
                        String passwordField = arguments.containsKey("passwordField")
                                ? (String) arguments.get("passwordField")
                                : "password";

                        Map<String, Object> config = new LinkedHashMap<>();
                        config.put("enabled", true);
                        config.put("authenticationType", authType);
                        config.put("useDbAuth", true);
                        config.put("principalEntity", principalEntity);
                        config.put("usernameField", usernameField);
                        config.put("passwordField", passwordField);
                        config.put("rbacMode", rbacMode);

                        if ("STATIC".equalsIgnoreCase(rbacMode)) {
                            config.put("permissions", List.of(
                                    "USER_READ", "USER_WRITE", "USER_DELETE",
                                    "ADMIN_READ", "ADMIN_WRITE", "ADMIN_DELETE"
                            ));
                            config.put("definedRoles", List.of(
                                    Map.of("name", "USER", "permissions", List.of("USER_READ", "USER_WRITE")),
                                    Map.of("name", "ADMIN", "permissions", List.of(
                                            "USER_READ", "USER_WRITE", "USER_DELETE",
                                            "ADMIN_READ", "ADMIN_WRITE", "ADMIN_DELETE"))
                            ));
                        }

                        if ("ENTITY".equalsIgnoreCase(rbacMode) || "DYNAMIC".equalsIgnoreCase(rbacMode)) {
                            config.put("roleEntity", "Role");
                        }

                        config.put("roleStrategy", "DYNAMIC".equalsIgnoreCase(rbacMode) ? "ENTITY" : "STRING");

                        Map<String, Object> result = new LinkedHashMap<>();
                        result.put("securityConfig", config);
                        result.put("usage", "Include this securityConfig object in your projectRequest when calling preview_project or generate_project.");

                        String json = objectMapper.writerWithDefaultPrettyPrinter()
                                .writeValueAsString(result);

                        return new McpSchema.CallToolResult(
                                List.of(new McpSchema.TextContent(json)), false);
                    } catch (Exception e) {
                        return new McpSchema.CallToolResult(
                                List.of(new McpSchema.TextContent("Error configuring security: " + e.getMessage())), true);
                    }
                }
        );
    }
}
