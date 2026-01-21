package com.firas.generator.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO containing all generated project files for preview.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectPreviewResponse {
    /**
     * List of all generated files with their paths and contents
     */
    private List<FilePreview> files;
}
