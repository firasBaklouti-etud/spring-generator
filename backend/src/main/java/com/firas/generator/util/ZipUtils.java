package com.firas.generator.util;

import com.firas.generator.model.FilePreview;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Utility class for creating ZIP archives.
 * 
 * This class provides methods to:
 * - Recursively zip a directory and all its contents
 * - Create a ZIP from a list of FilePreview objects
 * 
 * @author Firas Baklouti
 * @version 2.0
 * @since 2025-12-01
 */
public class ZipUtils {

    /**
     * Zips a directory and all its contents into a byte array.
     * 
     * This method recursively walks through the directory tree, adding all files
     * to the ZIP archive while preserving the directory structure. The paths in
     * the ZIP file are relative to the parent of the source directory.
     * 
     * @param sourceDir The directory to zip
     * @return Byte array containing the ZIP file content
     * @throws IOException If an error occurs during file reading or ZIP creation
     */
    public static byte[] zipDirectory(File sourceDir) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            Path sourcePath = sourceDir.toPath();
            Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    // Create ZIP entry with path relative to parent directory
                    Path targetFile = sourcePath.getParent().relativize(file);
                    zos.putNextEntry(new ZipEntry(targetFile.toString().replace("\\", "/")));
                    Files.copy(file, zos);
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }
            });
            
            zos.finish();
            return baos.toByteArray();
        }
    }
    
    /**
     * Creates a ZIP file from a list of FilePreview objects.
     * 
     * This method is used when the user has edited files in the IDE and wants
     * to download them as a complete project. It creates the ZIP in memory
     * without writing to disk.
     * 
     * @param files List of FilePreview objects with paths and contents
     * @param projectName Name of the root folder in the ZIP
     * @return Byte array containing the ZIP file content
     * @throws IOException If an error occurs during ZIP creation
     */
    public static byte[] createZipFromFilePreviews(List<FilePreview> files, String projectName) throws IOException {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("Files list cannot be null or empty");
        }
        
        if (projectName == null || projectName.trim().isEmpty()) {
            throw new IllegalArgumentException("Project name cannot be null or empty");
        }
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {
            
            for (FilePreview file : files) {
                if (file == null || file.getPath() == null) {
                    continue;
                }
                
                // Normalize path
                String filePath = file.getPath().replace("\\", "/");
                if (filePath.startsWith("/")) {
                    filePath = filePath.substring(1);
                }
                
                // Create entry with project name prefix
                String entryPath = projectName + "/" + filePath;
                zos.putNextEntry(new ZipEntry(entryPath));
                
                // Write content
                String content = file.getContent() != null ? file.getContent() : "";
                zos.write(content.getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
            
            zos.finish();
            return baos.toByteArray();
        }
    }
}

