package com.firas.generator.service;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.Map;

/**
 * Service for processing FreeMarker templates to generate code files.
 * 
 * This service uses FreeMarker template engine to generate various code files
 * (Java classes, configuration files, etc.) from templates. It handles template
 * loading, processing, and file writing with proper error handling.
 * 
 * Enhanced to support stack-specific templates organized in subdirectories
 * (e.g., templates/spring/, templates/node/).
 * 
 * @author Firas Baklouti
 * @version 2.0
 * @since 2025-12-01
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TemplateService {

    /** FreeMarker configuration for template processing */
    private final Configuration freemarkerConfig;

    /**
     * Generates a file from a FreeMarker template.
     * 
     * This method loads the specified template, processes it with the provided data model,
     * and writes the result to the output file. The parent directories of the output file
     * are created automatically if they don't exist.
     * 
     * @param templateName Name of the template file (e.g., "Entity.ftl" or "spring/Entity.ftl")
     * @param model Data model containing variables to be used in the template
     * @param outputFile The file where the generated content will be written
     * @throws RuntimeException if template processing or file writing fails
     */
    public void generateFile(String templateName, Map<String, Object> model, File outputFile) {
        try (Writer writer = new FileWriter(outputFile)) {
            Template template = freemarkerConfig.getTemplate(templateName);
            template.process(model, writer);
        } catch (IOException | TemplateException e) {
            log.error("Error generating file from template: {}", templateName, e);
            throw new RuntimeException("Failed to generate file", e);
        }
    }
    
    /**
     * Processes a template and writes the result to a Writer.
     * 
     * This method is useful for in-memory generation where you don't want
     * to write directly to a file (e.g., for preview generation).
     * 
     * @param templateName Name of the template file (e.g., "spring/Entity.ftl")
     * @param model Data model containing variables to be used in the template
     * @param writer The writer where the generated content will be written
     * @throws RuntimeException if template processing fails
     */
    public void processTemplate(String templateName, Map<String, Object> model, Writer writer) {
        try {
            Template template = freemarkerConfig.getTemplate(templateName);
            template.process(model, writer);
        } catch (IOException | TemplateException e) {
            log.error("Error processing template: {}", templateName, e);
            throw new RuntimeException("Failed to process template", e);
        }
    }
    
    /**
     * Processes a template and returns the result as a String.
     * 
     * @param templateName Name of the template file
     * @param model Data model for the template
     * @return The generated content as a string
     * @throws RuntimeException if template processing fails
     */
    public String processTemplateToString(String templateName, Map<String, Object> model) {
        StringWriter writer = new StringWriter();
        processTemplate(templateName, model, writer);
        return writer.toString();
    }
}

