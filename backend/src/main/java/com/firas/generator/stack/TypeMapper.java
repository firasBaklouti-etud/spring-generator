package com.firas.generator.stack;

import java.util.Map;

/**
 * Interface for mapping SQL data types to language-specific types.
 * 
 * Each stack implementation provides its own TypeMapper to convert
 * database column types (VARCHAR, BIGINT, etc.) to the target language's
 * type system (String/Long for Java, string/number for TypeScript, etc.).
 * 
 * This abstraction allows the SQL parser to remain stack-agnostic while
 * enabling proper type conversion during code generation.
 * 
 * @author Firas Baklouti
 * @version 1.0
 * @since 2025-12-07
 */
public interface TypeMapper {
    
    /**
     * Maps a SQL data type to the corresponding language-specific type.
     * 
     * @param sqlType The SQL type (e.g., "VARCHAR", "BIGINT", "TIMESTAMP")
     * @return The language-specific type (e.g., "String", "Long", "LocalDateTime")
     */
    String mapSqlType(String sqlType);
    
    /**
     * @return The default type for primary key IDs (e.g., "Long" for Java)
     */
    String getIdType();
    
    /**
     * @return The type for string/text data (e.g., "String" for Java)
     */
    String getStringType();
    
    /**
     * @return The type for boolean values (e.g., "Boolean" for Java)
     */
    String getBooleanType();
    
    /**
     * @return The type for date-only values (e.g., "LocalDate" for Java)
     */
    String getDateType();
    
    /**
     * @return The type for timestamp values (e.g., "LocalDateTime" for Java)
     */
    String getTimestampType();
    
    /**
     * @return The type for decimal/money values (e.g., "BigDecimal" for Java)
     */
    String getDecimalType();
    
    /**
     * @return The type for integer values (e.g., "Integer" for Java)
     */
    String getIntegerType();
    
    /**
     * @return Complete mapping of SQL types to language types
     */
    Map<String, String> getAllMappings();
}
