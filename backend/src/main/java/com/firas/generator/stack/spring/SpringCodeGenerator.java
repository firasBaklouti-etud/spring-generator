package com.firas.generator.stack.spring;

import com.firas.generator.model.FilePreview;
import com.firas.generator.model.Table;
import com.firas.generator.service.TemplateService;
import com.firas.generator.stack.CodeGenerator;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Code generator for Spring Boot projects.
 * 
 * Generates JPA entities, Spring Data repositories, services, and REST controllers
 * from table metadata using FreeMarker templates.
 * 
 * @author Firas Baklouti
 * @version 1.0
 * @since 2025-12-07
 */
@Component
public class SpringCodeGenerator implements CodeGenerator {
    
    private static final String TEMPLATE_DIR = "spring/";
    
    private final TemplateService templateService;
    
    public SpringCodeGenerator(TemplateService templateService) {
        this.templateService = templateService;
    }
    
    @Override
    public FilePreview generateEntity(Table table, String packageName) {
        Map<String, Object> model = createModel(table, packageName);
        
        String content = templateService.processTemplateToString(TEMPLATE_DIR + "Entity.ftl", model);
        String path = "src/main/java/" + packageName.replace(".", "/") + "/entity/" + table.getClassName() + ".java";
        
        return new FilePreview(path, content, "java");
    }
    
    @Override
    public FilePreview generateRepository(Table table, String packageName) {
        Map<String, Object> model = createModel(table, packageName);
        
        String content = templateService.processTemplateToString(TEMPLATE_DIR + "Repository.ftl", model);
        String path = "src/main/java/" + packageName.replace(".", "/") + "/repository/" + table.getClassName() + "Repository.java";
        
        return new FilePreview(path, content, "java");
    }
    
    @Override
    public FilePreview generateService(Table table, String packageName) {
        Map<String, Object> model = createModel(table, packageName);
        
        String content = templateService.processTemplateToString(TEMPLATE_DIR + "Service.ftl", model);
        String path = "src/main/java/" + packageName.replace(".", "/") + "/service/" + table.getClassName() + "Service.java";
        
        return new FilePreview(path, content, "java");
    }
    
    @Override
    public FilePreview generateController(Table table, String packageName) {
        Map<String, Object> model = createModel(table, packageName);
        
        String content = templateService.processTemplateToString(TEMPLATE_DIR + "Controller.ftl", model);
        String path = "src/main/java/" + packageName.replace(".", "/") + "/controller/" + table.getClassName() + "Controller.java";
        
        return new FilePreview(path, content, "java");
    }
    
    @Override
    public FilePreview generateDto(Table table, String packageName) {
        Map<String, Object> model = createModel(table, packageName);
        
        // TODO: Add Dto.ftl template for Spring
        String content = "// DTO for " + table.getClassName() + "\n// TODO: Implement DTO template";
        String path = "src/main/java/" + packageName.replace(".", "/") + "/dto/" + table.getClassName() + "Dto.java";
        
        return new FilePreview(path, content, "java");
    }
    
    @Override
    public FilePreview generateMapper(Table table, String packageName) {
        Map<String, Object> model = createModel(table, packageName);
        
        // TODO: Add Mapper.ftl template for Spring
        String content = "// Mapper for " + table.getClassName() + "\n// TODO: Implement Mapper template";
        String path = "src/main/java/" + packageName.replace(".", "/") + "/mapper/" + table.getClassName() + "Mapper.java";
        
        return new FilePreview(path, content, "java");
    }
    
    /**
     * Creates the template data model for a table.
     */
     private Map<String, Object> createModel(Table table, String packageName) {
        Map<String, Object> model = new HashMap<>();
        model.put("table", table);
        model.put("packageName", packageName);


        if (table.getMetadata() != null) {
            System.out.println("DEBUG: Table " + table.getName() + " has metadata: " + table.getMetadata());
            model.putAll(table.getMetadata());
        } else {
            System.out.println("DEBUG: Table " + table.getName() + " has NULL metadata");
        }
        
        return model;

    }
}
