package com.firas.generator.frontend.nextjs;

import com.firas.generator.frontend.FrontendProvider;
import com.firas.generator.model.Column;
import com.firas.generator.model.FilePreview;
import com.firas.generator.model.ProjectRequest;
import com.firas.generator.model.Table;
import com.firas.generator.model.config.FrontendConfig;
import com.firas.generator.model.config.SecurityConfig;
import com.firas.generator.service.TemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Frontend provider for Next.js project generation.
 * Generates a complete Next.js 14 application with TypeScript, Tailwind CSS,
 * and full CRUD pages for each entity.
 *
 * @author Firas Baklouti
 * @version 1.0
 * @since 2025-12-07
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class NextJsFrontendProvider implements FrontendProvider {

    private static final String TEMPLATE_DIR = "frontend/nextjs/";
    private static final String OUTPUT_PREFIX = "frontend/";

    private final TemplateService templateService;

    @Override
    public String getFramework() {
        return "NEXTJS";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public List<FilePreview> generateFrontend(ProjectRequest request) throws IOException {
        List<FilePreview> files = new ArrayList<>();

        FrontendConfig frontendConfig = request.getEffectiveFrontendConfig();
        SecurityConfig securityConfig = request.getSecurityConfig();

        List<Table> allTables = request.getTables() != null ? request.getTables() : Collections.emptyList();
        List<Table> tables = allTables.stream()
                .filter(t -> !t.isJoinTable())
                .collect(Collectors.toList());

        boolean hasSecurity = securityConfig != null && securityConfig.isEnabled();
        boolean hasJwt = hasSecurity && "JWT".equalsIgnoreCase(securityConfig.getAuthenticationType());

        List<Map<String, Object>> entities = tables.stream().map(t -> {
            Map<String, Object> entity = new HashMap<>();
            entity.put("name", t.getClassName());
            entity.put("route", t.getName().toLowerCase());
            entity.put("tableName", t.getName());
            entity.put("className", t.getClassName());
            entity.put("columns", t.getColumns());
            entity.put("relationships", t.getRelationships());
            return entity;
        }).collect(Collectors.toList());

        Map<String, Object> model = new HashMap<>();
        model.put("projectName", request.getName() != null ? request.getName() : "my-app");
        model.put("tables", tables);
        model.put("allTables", allTables);
        model.put("backendUrl", frontendConfig.getBackendUrl());
        model.put("port", frontendConfig.getPort());
        model.put("hasSecurity", hasSecurity);
        model.put("hasJwt", hasJwt);
        model.put("securityConfig", securityConfig);
        model.put("entities", entities);

        // 1. Config files
        files.add(generate("package.json.ftl", model, "package.json", "json"));
        files.add(generate("tsconfig.json.ftl", model, "tsconfig.json", "json"));
        files.add(generate("tailwind.config.ts.ftl", model, "tailwind.config.ts", "typescript"));
        files.add(generate("next.config.ts.ftl", model, "next.config.ts", "typescript"));
        files.add(generate("postcss.config.mjs.ftl", model, "postcss.config.mjs", "javascript"));
        files.add(generate("gitignore.ftl", model, ".gitignore", "text"));
        files.add(generate("env.local.ftl", model, ".env.local", "text"));

        // 2. App skeleton
        files.add(generate("layout.tsx.ftl", model, "app/layout.tsx", "typescriptreact"));
        files.add(generate("page.tsx.ftl", model, "app/page.tsx", "typescriptreact"));
        files.add(generate("globals.css.ftl", model, "app/globals.css", "css"));

        // 3. API client
        files.add(generate("api.ts.ftl", model, "lib/api.ts", "typescript"));

        // 4. TypeScript types
        files.add(generate("types.ts.ftl", model, "types/index.ts", "typescript"));

        // 5. UI components
        files.add(generate("button.tsx.ftl", model, "components/ui/button.tsx", "typescriptreact"));
        files.add(generate("data-table.tsx.ftl", model, "components/ui/data-table.tsx", "typescriptreact"));
        files.add(generate("form-field.tsx.ftl", model, "components/ui/form-field.tsx", "typescriptreact"));
        files.add(generate("modal.tsx.ftl", model, "components/ui/modal.tsx", "typescriptreact"));

        // 6. Navigation
        files.add(generate("navbar.tsx.ftl", model, "components/navbar.tsx", "typescriptreact"));

        // 7. Per-entity CRUD pages
        for (Table table : tables) {
            Map<String, Object> entityModel = new HashMap<>(model);
            entityModel.put("table", table);
            entityModel.put("entity", buildEntityMap(table));
            String route = table.getName().toLowerCase();

            files.add(generate("entity-list-page.tsx.ftl", entityModel,
                    "app/" + route + "/page.tsx", "typescriptreact"));
            files.add(generate("entity-create-page.tsx.ftl", entityModel,
                    "app/" + route + "/new/page.tsx", "typescriptreact"));
            files.add(generate("entity-detail-page.tsx.ftl", entityModel,
                    "app/" + route + "/[id]/page.tsx", "typescriptreact"));
            files.add(generate("entity-edit-page.tsx.ftl", entityModel,
                    "app/" + route + "/[id]/edit/page.tsx", "typescriptreact"));
        }

        // 8. Auth pages (if security enabled)
        if (hasSecurity) {
            files.add(generate("auth.ts.ftl", model, "lib/auth.ts", "typescript"));
            files.add(generateAuthPage("login", model));
            if (securityConfig.isRegistrationEnabled()) {
                files.add(generateAuthPage("register", model));
            }
        }

        log.info("Generated {} Next.js frontend files", files.size());
        return files;
    }

    private Map<String, Object> buildEntityMap(Table table) {
        Map<String, Object> entity = new HashMap<>();
        entity.put("name", table.getClassName());
        entity.put("route", table.getName().toLowerCase());
        entity.put("tableName", table.getName());
        entity.put("className", table.getClassName());
        entity.put("columns", table.getColumns());
        entity.put("relationships", table.getRelationships());

        // Find the primary key column
        Column pk = table.getColumns().stream()
                .filter(Column::isPrimaryKey)
                .findFirst()
                .orElse(null);
        entity.put("pkField", pk != null ? pk.getFieldName() : "id");
        entity.put("pkType", pk != null ? mapToTsType(pk.getJavaType()) : "number");

        return entity;
    }

    private String mapToTsType(String javaType) {
        if (javaType == null) return "string";
        return switch (javaType) {
            case "Long", "Integer", "int", "long", "Double", "Float", "BigDecimal", "double", "float" -> "number";
            case "Boolean", "boolean" -> "boolean";
            default -> "string";
        };
    }

    private FilePreview generate(String templateName, Map<String, Object> model,
                                  String outputPath, String language) {
        String content = templateService.processTemplateToString(TEMPLATE_DIR + templateName, model);
        return new FilePreview(OUTPUT_PREFIX + outputPath, content, language);
    }

    private FilePreview generateAuthPage(String page, Map<String, Object> model) {
        String templateName = page + "-page.tsx.ftl";
        String content = templateService.processTemplateToString(TEMPLATE_DIR + templateName, model);
        return new FilePreview(OUTPUT_PREFIX + "app/" + page + "/page.tsx", content, "typescriptreact");
    }
}
