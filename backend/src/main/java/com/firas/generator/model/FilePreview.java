package com.firas.generator.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a file preview with its path, content, and programming language.
 * Used to send generated project files to the frontend for IDE preview.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FilePreview {
    /**
     * Relative file path within the project (e.g., "src/main/java/com/example/Demo.java")
     */
    private String path;
    
    /**
     * Full content of the file
     */
    private String content;
    
    /**
     * Programming language inferred from file extension (e.g., "java", "xml", "properties")
     */
    private String language;
}
