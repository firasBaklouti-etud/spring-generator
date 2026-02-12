package com.firas.generator.mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.firas.generator.mcp.McpToolProvider;
import com.firas.generator.model.ProjectRequest;
import com.firas.generator.stack.StackProvider;
import com.firas.generator.stack.StackProviderFactory;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * MCP tool that generates a complete project as a base64-encoded ZIP file.
 */
@Component
public class GenerateProjectTool implements McpToolProvider {

    private static final Logger log = LoggerFactory.getLogger(GenerateProjectTool.class);

    private final StackProviderFactory stackProviderFactory;
    private final ObjectMapper objectMapper;

    public GenerateProjectTool(StackProviderFactory stackProviderFactory, ObjectMapper objectMapper) {
        this.stackProviderFactory = stackProviderFactory;
        this.objectMapper = objectMapper;
    }

    @Override
    public McpServerFeatures.SyncToolSpecification getToolSpecification() {
        McpSchema.JsonSchema inputSchema = new McpSchema.JsonSchema(
                "object",
                Map.of(
                        "projectRequest", Map.of("type", "object",
                                "description", "Full project request object. Same structure as preview_project.")
                ),
                List.of("projectRequest"),
                null, null, null
        );

        McpSchema.Tool tool = McpSchema.Tool.builder()
                .name("generate_project")
                .description("Generate a complete project as a base64-encoded ZIP file. " +
                        "Returns JSON with filename, encoding, size, and base64 content. " +
                        "Use preview_project first to review files before generating the final ZIP.")
                .inputSchema(inputSchema)
                .build();

        return new McpServerFeatures.SyncToolSpecification(tool, (exchange, arguments) -> {
            try {
                Object requestObj = arguments.get("projectRequest");
                ProjectRequest request = objectMapper.convertValue(requestObj, ProjectRequest.class);

                StackProvider provider = stackProviderFactory.getProvider(request.getStackType());
                byte[] zipBytes = provider.generateProjectZip(request);
                String base64 = Base64.getEncoder().encodeToString(zipBytes);

                String artifactId = "SPRING".equalsIgnoreCase(request.getStackType().name())
                        && request.getEffectiveSpringConfig() != null
                        ? request.getEffectiveSpringConfig().getArtifactId()
                        : request.getName() != null
                            ? request.getName().toLowerCase().replaceAll("\\s+", "-")
                            : "project";

                String response = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(Map.of(
                        "filename", artifactId + ".zip",
                        "encoding", "base64",
                        "size", zipBytes.length,
                        "content", base64
                ));

                return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(response)), false);
            } catch (Exception e) {
                log.error("Failed to generate project", e);
                return new McpSchema.CallToolResult(
                        List.of(new McpSchema.TextContent("Error generating project: " + e.getMessage())), true);
            }
        });
    }
}
