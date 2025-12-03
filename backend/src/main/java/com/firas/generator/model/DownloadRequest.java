package com.firas.generator.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for downloading a project from edited files.
 * Contains the list of files (potentially modified by the user) and the artifact ID for naming the ZIP.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DownloadRequest {
    /**
     * List of files to include in the ZIP (may include user edits from the IDE preview)
     */
    private List<FilePreview> files;
    
    /**
     * Artifact ID used to name the downloaded ZIP file
     */
    private String artifactId;
}
