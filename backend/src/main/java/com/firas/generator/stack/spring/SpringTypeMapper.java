package com.firas.generator.stack.spring;

import com.firas.generator.stack.TypeMapper;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Type mapper for Spring Boot / Java.
 * 
 * Converts SQL database types to Java types used in JPA entities.
 * 
 * @author Firas Baklouti
 * @version 1.0
 * @since 2025-12-07
 */
@Component
public class SpringTypeMapper implements TypeMapper {
    
    private static final Map<String, String> TYPE_MAPPINGS;
    
    static {
        Map<String, String> mappings = new HashMap<>();
        
        // String types
        mappings.put("VARCHAR", "String");
        mappings.put("TEXT", "String");
        mappings.put("CHAR", "String");
        mappings.put("LONGTEXT", "String");
        mappings.put("MEDIUMTEXT", "String");
        mappings.put("TINYTEXT", "String");
        mappings.put("CLOB", "String");
        
        // Integer types
        mappings.put("INT", "Integer");
        mappings.put("INTEGER", "Integer");
        mappings.put("SMALLINT", "Integer");
        mappings.put("TINYINT", "Integer");
        mappings.put("MEDIUMINT", "Integer");
        
        // Long types
        mappings.put("BIGINT", "Long");
        
        // Floating point types
        mappings.put("DOUBLE", "Double");
        mappings.put("FLOAT", "Double");
        mappings.put("REAL", "Double");
        
        // Decimal types
        mappings.put("DECIMAL", "BigDecimal");
        mappings.put("NUMERIC", "BigDecimal");
        mappings.put("MONEY", "BigDecimal");
        
        // Boolean types
        mappings.put("BOOLEAN", "Boolean");
        mappings.put("BOOL", "Boolean");
        mappings.put("BIT", "Boolean");
        
        // Date/Time types
        mappings.put("DATE", "LocalDate");
        mappings.put("TIME", "LocalTime");
        mappings.put("TIMESTAMP", "LocalDateTime");
        mappings.put("DATETIME", "LocalDateTime");
        
        // Binary types
        mappings.put("BLOB", "byte[]");
        mappings.put("BINARY", "byte[]");
        mappings.put("VARBINARY", "byte[]");
        mappings.put("LONGBLOB", "byte[]");
        
        // UUID
        mappings.put("UUID", "UUID");
        
        TYPE_MAPPINGS = Collections.unmodifiableMap(mappings);
    }
    
    @Override
    public String mapSqlType(String sqlType) {
        if (sqlType == null) {
            return getStringType();
        }
        
        // Extract base type (remove parentheses like VARCHAR(255))
        String baseType = sqlType.toUpperCase().replaceAll("\\(.*\\)", "").trim();
        
        return TYPE_MAPPINGS.getOrDefault(baseType, getStringType());
    }
    
    @Override
    public String getIdType() {
        return "Long";
    }
    
    @Override
    public String getStringType() {
        return "String";
    }
    
    @Override
    public String getBooleanType() {
        return "Boolean";
    }
    
    @Override
    public String getDateType() {
        return "LocalDate";
    }
    
    @Override
    public String getTimestampType() {
        return "LocalDateTime";
    }
    
    @Override
    public String getDecimalType() {
        return "BigDecimal";
    }
    
    @Override
    public String getIntegerType() {
        return "Integer";
    }
    
    @Override
    public Map<String, String> getAllMappings() {
        return TYPE_MAPPINGS;
    }
}
