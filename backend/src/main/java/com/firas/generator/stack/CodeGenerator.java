package com.firas.generator.stack;

import com.firas.generator.model.FilePreview;
import com.firas.generator.model.ProjectRequest;
import com.firas.generator.model.Table;

import java.util.List;

/**
 * Interface for generating code files from templates.
 * 
 * Each stack implementation provides its own CodeGenerator to produce
 * stack-specific code files (entities, repositories, services, controllers).
 * The generator uses the stack's TypeMapper for proper type conversion
 * and the TemplateService for file generation.
 * 
 * This abstraction enables the Template Method pattern in AbstractStackProvider,
 * where the generation workflow is fixed but individual file generation is delegated.
 * 
 * @author Firas Baklouti
 * @version 1.0
 * @since 2025-12-07
 */
public interface CodeGenerator {
    
    /**
     * Generates an entity/model class for the given table.
     * 
     * For Spring: JPA @Entity class
     * For Node: Prisma model or Sequelize model
     * For Nest: TypeORM entity
     * For FastAPI: SQLAlchemy model
     * 
     * @param table The table metadata
     * @param packageName The base package/module name
     * @return FilePreview containing the generated file
     */
    FilePreview generateEntity(Table table, String packageName);
    
    /**
     * Generates a repository/data access class for the given table.
     * 
     * For Spring: JpaRepository interface
     * For Node: Prisma client wrapper or DAO
     * For Nest: TypeORM repository
     * For FastAPI: SQLAlchemy CRUD operations
     * 
     * @param table The table metadata
     * @param packageName The base package/module name
     * @return FilePreview containing the generated file
     */
    FilePreview generateRepository(Table table, String packageName);
    
    /**
     * Generates a service/business logic class for the given table.
     * 
     * @param table The table metadata
     * @param packageName The base package/module name
     * @return FilePreview containing the generated file
     */
    FilePreview generateService(Table table, String packageName);
    
    /**
     * Generates a controller/router for the given table.
     * 
     * For Spring: REST @RestController
     * For Node: Express router
     * For Nest: @Controller class
     * For FastAPI: APIRouter
     * 
     * @param table The table metadata
     * @param packageName The base package/module name
     * @return FilePreview containing the generated file
     */
    FilePreview generateController(Table table, String packageName);
    
    /**
     * Generates a DTO (Data Transfer Object) class for the given table.
     * 
     * @param table The table metadata
     * @param packageName The base package/module name
     * @return FilePreview containing the generated file
     */
    FilePreview generateDto(Table table, String packageName);
    
    /**
     * Generates a mapper class for entity-DTO conversion.
     * 
     * @param table The table metadata
     * @param packageName The base package/module name
     * @return FilePreview containing the generated file
     */
    FilePreview generateMapper(Table table, String packageName);
    
    /**
     * Generates all CRUD code files for the given tables based on request flags.
     * 
     * This is a convenience method that generates entities, repositories,
     * services, and controllers based on the flags in ProjectRequest.
     * 
     * @param request The project request containing tables and generation flags
     * @return List of all generated FilePreview objects
     */
    default List<FilePreview> generateAllCrud(ProjectRequest request) {
        java.util.List<FilePreview> files = new java.util.ArrayList<>();
        
        if (request.getTables() == null || request.getTables().isEmpty()) {
            return files;
        }
        
        for (Table table : request.getTables()) {
            if (table.isJoinTable()) {
                continue; // Skip join tables
            }
            
            if (request.isIncludeEntity()) {
                files.add(generateEntity(table, request.getPackageName()));
            }
            if (request.isIncludeRepository()) {
                files.add(generateRepository(table, request.getPackageName()));
            }
            if (request.isIncludeService()) {
                files.add(generateService(table, request.getPackageName()));
            }
            if (request.isIncludeController()) {
                files.add(generateController(table, request.getPackageName()));
            }
            if (request.isIncludeDto()) {
                files.add(generateDto(table, request.getPackageName()));
            }
            if (request.isIncludeMapper()) {
                files.add(generateMapper(table, request.getPackageName()));
            }
        }
        
        return files;
    }
}
