# Spring Boot Generator - Backend Documentation

## Table of Contents
1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Getting Started](#getting-started)
4. [Project Structure](#project-structure)
5. [Core Components](#core-components)
6. [API Endpoints](#api-endpoints)
7. [Code Generation Flow](#code-generation-flow)
8. [SQL Parser](#sql-parser)
9. [Template System](#template-system)
10. [Development Guide](#development-guide)
11. [Testing](#testing)
12. [Troubleshooting](#troubleshooting)

---

## Overview

The Spring Boot Generator is a web-based tool that generates customized Spring Boot projects with:
- **Dynamic dependency management** fetched from Spring Initializr API
- **Automatic CRUD generation** from SQL schemas
- **Template-based code generation** using FreeMarker
- **Complete project structure** with Maven configuration

### Key Features
- ✅ Generate Spring Boot projects with selected dependencies
- ✅ Parse SQL CREATE TABLE and ALTER TABLE statements
- ✅ Auto-generate JPA entities, repositories, services, and controllers
- ✅ Detect and generate JPA relationships (OneToOne, OneToMany, ManyToOne, ManyToMany)
- ✅ Download projects as ZIP files
- ✅ Support for Java 17+ and Spring Boot 3.x

---

## Architecture

### Technology Stack
- **Framework**: Spring Boot 3.2.0
- **Java Version**: 17
- **Build Tool**: Maven
- **Template Engine**: FreeMarker
- **HTTP Client**: WebClient (Spring WebFlux)
- **Utilities**: Apache Commons IO, Lombok

### Design Pattern
The application follows a layered architecture:

```
┌─────────────────────────────────────┐
│         Controllers                 │  ← REST API Layer
├─────────────────────────────────────┤
│          Services                   │  ← Business Logic
├─────────────────────────────────────┤
│       Utilities & Parsers           │  ← Helper Components
├─────────────────────────────────────┤
│      Models & DTOs                  │  ← Data Transfer Objects
└─────────────────────────────────────┘
```

---

## Getting Started

### Prerequisites
- Java 17 or higher
- Maven 3.6+
- IDE (IntelliJ IDEA, Eclipse, or VS Code)
- Git

### Installation Steps

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd springInitializer/backend
   ```

2. **Build the project**
   ```bash
   mvn clean install
   ```

3. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

4. **Access the API**
   - Base URL: `http://localhost:8080`
   - Health check: `http://localhost:8080/actuator/health` (if actuator is enabled)

### Quick Test
```bash
# Get available dependencies
curl http://localhost:8080/api/dependencies/groups

# Generate a simple project (POST request with JSON body)
curl -X POST http://localhost:8080/api/generate/project \
  -H "Content-Type: application/json" \
  -d '{
    "groupId": "com.example",
    "artifactId": "demo",
    "name": "Demo",
    "description": "Demo project",
    "packageName": "com.example.demo",
    "javaVersion": "17",
    "bootVersion": "3.2.0",
    "dependencies": ["web", "jpa"]
  }' \
  --output demo.zip
```

---

## Project Structure

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/firas/generator/
│   │   │   ├── BackendApplication.java          # Main entry point
│   │   │   ├── controller/
│   │   │   │   ├── DependencyController.java    # Dependency API
│   │   │   │   ├── GeneratorController.java     # Project generation API
│   │   │   │   └── SqlParserController.java     # SQL parsing API
│   │   │   ├── model/
│   │   │   │   ├── Column.java                  # Database column model
│   │   │   │   ├── DependencyGroup.java         # Dependency grouping
│   │   │   │   ├── DependencyMetadata.java      # Dependency details
│   │   │   │   ├── ProjectRequest.java          # Generation request DTO
│   │   │   │   ├── Relationship.java            # JPA relationship model
│   │   │   │   ├── RelationshipType.java        # Relationship enum
│   │   │   │   └── Table.java                   # Database table model
│   │   │   ├── service/
│   │   │   │   ├── DependencyRegistry.java      # Dependency management
│   │   │   │   ├── ProjectGeneratorService.java # Generation interface
│   │   │   │   ├── TemplateService.java         # Template processing
│   │   │   │   └── impl/
│   │   │   │       └── ProjectGeneratorServiceImpl.java
│   │   │   └── util/
│   │   │       ├── SqlParser.java               # SQL schema parser
│   │   │       └── ZipUtils.java                # ZIP file utilities
│   │   └── resources/
│   │       ├── application.properties           # App configuration
│   │       └── templates/                       # FreeMarker templates
│   │           ├── pom.xml.ftl
│   │           ├── Application.java.ftl
│   │           ├── Entity.ftl
│   │           ├── Repository.ftl
│   │           ├── Service.ftl
│   │           └── Controller.ftl
│   └── test/
│       └── java/                                # Unit tests
└── pom.xml                                      # Maven configuration
```

---

## Core Components

### 1. DependencyRegistry
**Purpose**: Fetches and manages Spring Boot dependencies from Spring Initializr API.

**Key Methods**:
- `initialize()`: Loads dependencies at startup
- `getAllGroups()`: Returns all dependency groups
- `resolveDependencies(List<String> ids)`: Converts dependency IDs to metadata

**Example**:
```java
@Autowired
private DependencyRegistry registry;

List<DependencyGroup> groups = registry.getAllGroups();
```

### 2. SqlParser
**Purpose**: Parses SQL schemas to extract table and column metadata.

**Supported SQL Statements**:
- `CREATE TABLE` with inline constraints
- `ALTER TABLE ADD PRIMARY KEY`
- `ALTER TABLE ADD FOREIGN KEY`
- `ALTER TABLE MODIFY COLUMN`
- `ALTER TABLE DROP COLUMN`
- `ALTER TABLE ADD UNIQUE`

**Example**:
```java
String sql = """
    CREATE TABLE users (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        username VARCHAR(50) UNIQUE NOT NULL,
        email VARCHAR(100)
    );
    """;

List<Table> tables = sqlParser.parseSql(sql);
```

### 3. ProjectGeneratorService
**Purpose**: Orchestrates the entire project generation process.

**Generation Steps**:
1. Create temporary directory
2. Generate Maven structure (src/main/java, src/main/resources, etc.)
3. Generate pom.xml with dependencies
4. Generate main Application class
5. Generate application.properties
6. (Optional) Generate CRUD code from SQL
7. ZIP the project
8. Clean up temporary files

### 4. TemplateService
**Purpose**: Processes FreeMarker templates to generate code files.

**Example**:
```java
Map<String, Object> model = new HashMap<>();
model.put("packageName", "com.example.demo");
model.put("className", "User");

templateService.generateFile("Entity.ftl", model, outputFile);
```

---

## API Endpoints

### 1. Get Dependency Groups
**Endpoint**: `GET /api/dependencies/groups`

**Response**:
```json
[
  {
    "name": "Web",
    "dependencies": [
      {
        "id": "web",
        "name": "Spring Web",
        "description": "Build web applications...",
        "groupId": "org.springframework.boot",
        "artifactId": "spring-boot-starter-web"
      }
    ]
  }
]
```

### 2. Parse SQL Schema
**Endpoint**: `GET /api/sqlParser/{sql}`

**Description**: Transforms SQL CREATE TABLE and ALTER TABLE statements into structured table metadata.

**Path Parameter**:
- `sql`: The SQL statements to parse (URL-encoded)

**Example Request**:
```bash
curl "http://localhost:8080/api/sqlParser/CREATE%20TABLE%20users%20(id%20BIGINT%20PRIMARY%20KEY%20AUTO_INCREMENT,%20username%20VARCHAR(50));"
```

**Response**:
```json
[
  {
    "name": "users",
    "className": "User",
    "columns": [
      {
        "name": "id",
        "fieldName": "id",
        "javaType": "Long",
        "sqlType": "BIGINT",
        "primaryKey": true,
        "autoIncrement": true
      },
      {
        "name": "username",
        "fieldName": "username",
        "javaType": "String",
        "sqlType": "VARCHAR",
        "length": 50
      }
    ],
    "relationships": []
  }
]
```

### 3. Generate Project
**Endpoint**: `POST /api/generate/project`

**Description**: Generates a complete Spring Boot project with optional CRUD code from table metadata.

**Request Body**:
```json
{
  "groupId": "com.example",
  "artifactId": "myapp",
  "name": "MyApp",
  "description": "My Spring Boot Application",
  "packageName": "com.example.myapp",
  "javaVersion": "17",
  "bootVersion": "3.2.0",
  "dependencies": [
    {
      "id": "web",
      "name": "Spring Web",
      "groupId": "org.springframework.boot",
      "artifactId": "spring-boot-starter-web",
      "scope": "compile",
      "isStarter": true
    }
  ],
  "includeEntity": true,
  "includeRepository": true,
  "includeService": true,
  "includeController": true,
  "includeDto": false,
  "includeMapper": false,
  "tables": [
    {
      "name": "users",
      "className": "User",
      "columns": [...],
      "relationships": [...]
    }
  ]
}
```

**Note**: The `tables` field should contain the table metadata returned from the SQL Parser endpoint.

**Response**: ZIP file download

### 4. Generate Project Preview (IDE Mode)
**Endpoint**: `POST /api/generate/preview`

**Description**: Generates project files in memory and returns them as a JSON structure for previewing.

**Request Body**: Same as `POST /api/generate/project`

**Response**:
```json
{
  "files": [
    {
      "path": "pom.xml",
      "content": "<project>...</project>",
      "language": "xml"
    },
    {
      "path": "src/main/java/com/example/DemoApplication.java",
      "content": "package com.example...",
      "language": "java"
    }
  ]
}
```

### 5. Generate ZIP from Files
**Endpoint**: `POST /api/generate/from-files`

**Description**: Accepts a list of files (potentially modified in the IDE) and returns them as a ZIP archive.

**Request Body**:
```json
{
  "artifactId": "demo",
  "files": [
    {
      "path": "pom.xml",
      "content": "...",
      "language": "xml"
    },
    ...
  ]
}
```

**Response**: ZIP file download

---

## Workflow: Two-Phase Project Generation

The system now uses a two-phase approach for generating projects with CRUD code:

### Phase 1: Parse SQL to Table Metadata
```bash
# Step 1: Parse SQL using the SqlParser endpoint
curl "http://localhost:8080/api/sqlParser/{url-encoded-sql}" > tables.json
```

### Phase 2: Generate Project with Tables
```bash
# Step 2: Generate project using the parsed table metadata
curl -X POST http://localhost:8080/api/generate/project \
  -H "Content-Type: application/json" \
  -d '{
    "groupId": "com.example",
    "artifactId": "demo",
    "name": "Demo",
    "description": "Demo project",
    "packageName": "com.example.demo",
    "javaVersion": "17",
    "bootVersion": "3.2.0",
    "dependencies": ["web", "jpa"],
    "tables": <paste-tables-from-phase1>
  }' \
  --output demo.zip
```

### Benefits of Two-Phase Approach
1. **Separation of Concerns**: SQL parsing is decoupled from project generation
2. **Flexibility**: Clients can modify table metadata before generating code
3. **Validation**: Clients can review and validate parsed tables before generation
4. **Reusability**: Parsed tables can be reused for multiple project generations

---

## Code Generation Flow

### Complete Workflow (Two-Phase)

```
User submits SQL
    ↓
┌─────────────────────────────────────┐
│   PHASE 1: SQL Parsing              │
└─────────────────────────────────────┘
    ↓
SqlParserController.parseSql()
    ↓
SqlParser.parseSql()
    ├─→ Parse CREATE TABLE statements
    ├─→ Parse ALTER TABLE statements
    ├─→ Detect relationships (FK, etc.)
    └─→ Build Table objects with metadata
    ↓
Return List<Table> (JSON)
    ↓
┌─────────────────────────────────────┐
│   PHASE 2: Project Generation       │
└─────────────────────────────────────┘
    ↓
User submits ProjectRequest with tables
    ↓
ProjectGeneratorServiceImpl.generateProject()
    ↓
1. Create temp directory
    ↓
2. Generate structure (src/main/java, src/main/resources)
    ↓
3. Generate pom.xml
    ├─→ Resolve dependencies from DependencyRegistry
    └─→ Process pom.xml.ftl template
    ↓
4. Generate Application.java
    └─→ Process Application.java.ftl template
    ↓
5. Generate application.properties
    └─→ Process application.properties.ftl template
    ↓
6. If tables provided:
    ├─→ Use pre-parsed Table objects
    ├─→ For each table:
    │   ├─→ Generate Entity.java
    │   ├─→ Generate Repository.java
    │   ├─→ Generate Service.java
    │   └─→ Generate Controller.java
    ↓
7. ZIP the project directory
    └─→ Use ZipUtils.zipDirectory()
    ↓
8. Clean up temp directory
    ↓
Return ZIP byte array
```

---

## SQL Parser

### Relationship Detection

The SQL parser automatically detects JPA relationships:

**OneToMany / ManyToOne**:
```sql
CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    user_id BIGINT,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
-- Creates: User.orders (OneToMany) and Order.user (ManyToOne)
```

**OneToOne**:
```sql
CREATE TABLE profiles (
    id BIGINT PRIMARY KEY,
    user_id BIGINT UNIQUE,  -- UNIQUE makes it OneToOne
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

**ManyToMany**:
```sql
-- Join table with exactly 2 foreign keys
CREATE TABLE user_roles (
    user_id BIGINT,
    role_id BIGINT,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (role_id) REFERENCES roles(id)
);
-- Creates: User.roles and Role.users (both ManyToMany)
```

### Supported Data Types

| SQL Type | Java Type |
|----------|-----------|
| VARCHAR, TEXT, CHAR | String |
| INT, INTEGER | Integer |
| BIGINT | Long |
| DOUBLE, FLOAT | Double |
| BOOLEAN, BIT | Boolean |
| DATE | LocalDate |
| TIMESTAMP, DATETIME | LocalDateTime |

---

## Template System

### Available Templates

Located in `src/main/resources/templates/`:

1. **pom.xml.ftl**: Maven configuration
2. **Application.java.ftl**: Main Spring Boot class
3. **application.properties.ftl**: Configuration file
4. **Entity.ftl**: JPA entity class
5. **Repository.ftl**: Spring Data repository
6. **Service.ftl**: Service layer
7. **Controller.ftl**: REST controller

### Template Variables

**Entity.ftl**:
- `table`: Table object with columns and relationships
- `packageName`: Base package name

**Example Entity Template Usage**:
```ftl
package ${packageName}.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "${table.name}")
public class ${table.className} {
    <#list table.columns as column>
    <#if column.primaryKey>
    @Id
    <#if column.autoIncrement>
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    </#if>
    </#if>
    private ${column.javaType} ${column.fieldName};
    </#list>
}
```

---

## Development Guide

### Adding a New Dependency Group

1. Modify `DependencyRegistry.java`:
```java
private void initializeStaticDependencies() {
    DependencyGroup myGroup = new DependencyGroup("My Group");
    myGroup.addDependency(createDependency(
        "my-dep", "My Dependency", "Description",
        "com.example", "my-artifact", "1.0.0", "compile"
    ));
    groups.add(myGroup);
}
```

### Adding a New Template

1. Create template file in `src/main/resources/templates/MyTemplate.ftl`
2. Use in service:
```java
templateService.generateFile("MyTemplate.ftl", model, outputFile);
```

### Extending SQL Parser

To support new SQL statements:

1. Add regex pattern:
```java
private static final Pattern MY_PATTERN = Pattern.compile(
    "MY_SQL_PATTERN", Pattern.CASE_INSENSITIVE
);
```

2. Add parsing logic in `parseSql()` method

### Custom Code Generation

Extend `ProjectGeneratorServiceImpl`:

```java
private void generateCustomCode(File projectDir, ProjectRequest request) {
    // Your custom generation logic
    Map<String, Object> model = new HashMap<>();
    model.put("customData", myData);
    
    templateService.generateFile("CustomTemplate.ftl", model, outputFile);
}
```

---

## Testing

### Unit Testing Example

```java
@SpringBootTest
class SqlParserTest {
    
    @Autowired
    private SqlParser sqlParser;
    
    @Test
    void testParseCreateTable() {
        String sql = "CREATE TABLE users (id BIGINT PRIMARY KEY);";
        List<Table> tables = sqlParser.parseSql(sql);
        
        assertEquals(1, tables.size());
        assertEquals("users", tables.get(0).getName());
    }
}
```

### Integration Testing

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class GeneratorControllerTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void testGenerateProject() {
        ProjectRequest request = new ProjectRequest();
        request.setArtifactId("test-app");
        // ... set other fields
        
        ResponseEntity<byte[]> response = restTemplate.postForEntity(
            "/api/generate/project", request, byte[].class
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }
}
```

---

## Troubleshooting

### Common Issues

**1. Dependencies not loading**
- Check internet connection
- Verify Spring Initializr API is accessible: `https://start.spring.io/metadata/config`
- Check logs for WebClient errors

**2. SQL parsing errors**
- Ensure SQL syntax is valid
- Check for unsupported SQL features
- Enable debug logging: `logging.level.com.firas.generator.util.SqlParser=DEBUG`

**3. Template processing errors**
- Verify template file exists in `src/main/resources/templates/`
- Check template syntax (FreeMarker)
- Review error logs for specific line numbers

**4. ZIP generation fails**
- Check disk space
- Verify write permissions on temp directory
- Review file paths for invalid characters

### Debug Mode

Enable detailed logging in `application.properties`:
```properties
logging.level.com.firas.generator=DEBUG
logging.level.org.springframework.web=DEBUG
```

### Useful Commands

```bash
# Clean build
mvn clean install -DskipTests

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Generate dependency tree
mvn dependency:tree

# Check for updates
mvn versions:display-dependency-updates
```

---

## AI Generation Features

### Overview

The backend now includes **AI-powered schema generation** using Google's **Agent Development Kit (ADK)**. This feature allows users to generate or modify database schemas using natural language prompts.

### Technology Stack

- **Google ADK 0.1.0**: Agent framework for LLM interactions
- **Gemini 2.0 Flash**: Language model for schema generation
- **RxJava 3**: Reactive event processing
- **Jackson**: JSON parsing and serialization

### Architecture

#### Components

1. **AIController** (`controller/AIController.java`)
   - REST endpoint for AI requests
   - Handles errors and returns structured responses

2. **AIGeneratedTablesService** (`service/AIGeneratedTablesService.java`)
   - Core AI service using Google ADK
   - LLM agent configuration
   - Session management
   - Action processing

3. **Model Classes** (`model/AI/`)
   - `AIGeneratedTables`: Response structure
   - `AIGeneratedTablesRequest`: Request payload
   - `TableAction`: Schema modification actions
   - `TableActionType`: Action types enum (create, edit, delete, replace)
   - `AIagent`: Agent interface

### API Endpoint

#### Generate Tables with AI
```
POST /api/ai/generateTables
Content-Type: application/json
```

**Request Body**:
```json
{
  "prompt": "Create users, products, orders, and order_items tables for e-commerce",
  "currentTables": [...],  // Array of existing Table objects
  "sessionId": "optional-session-id",
  "allowDestructive": false,
  "timestamp": 1701234567890
}
```

**Response**:
```json
{
  "sessionId": "session-abc123",
  "actions": [
    {
      "type": "create",
      "tables": [
        {
          "name": "users",
          "className": "User",
          "columns": [...],
          "relationships": [],
          "joinTable": false
        }
      ]
    }
  ],
  "explanation": "Created 4 tables for e-commerce: users, products, orders, and order_items with proper relationships."
}
```

### AI Provider Architecture

The backend implements a **flexible, multi-provider architecture** using the Factory pattern, allowing you to switch between different AI providers (Google ADK, OpenAI, Anthropic) without code changes.

#### Components

**1. AIProvider Interface** (`service/ai/AIProvider.java`)

Defines the contract for all AI providers:

```java
public interface AIProvider {
    AIGeneratedTables generateTables(AIGeneratedTablesRequest request);
    String getProviderName();
    boolean isAvailable();
}
```

**2. AIProviderFactory** (`service/ai/AIProviderFactory.java`)

Factory class for managing and creating AI provider instances:

```java
@Component
public class AIProviderFactory {
    private final Map<String, AIProvider> providers;
    
    // Auto-discovers all AI provider implementations via Spring dependency injection
    @Autowired
    public AIProviderFactory(List<AIProvider> providerList) {
        this.providers = providerList.stream()
            .collect(Collectors.toMap(
                AIProvider::getProviderName,
                Function.identity()
            ));
    }
    
    public AIProvider getProvider(String providerName);      // Get specific provider
    public AIProvider getDefaultProvider();                  // Get first available
    public List<String> getAvailableProviders();            // List all available
}
```

**3. Provider Implementations**

##### Google ADK Provider (`providers/GoogleADKProvider.java`)
- Uses Google's Agent Development Kit
- Model: `gemini-2.0-flash`
- Always available (no API key required)
- Provides detailed SQL schema instructions
- Reactive processing with RxJava

```java
@Component
public class GoogleADKProvider implements AIProvider {
    @Override
    public String getProviderName() { return "GOOGLE_ADK"; }
    
    @Override
    public boolean isAvailable() { return rootAgent != null; }
}
```

##### OpenAI Provider (`providers/OpenAIProvider.java`)
- Uses OpenAI Chat Completions API
- Default model: `gpt-4`
- Requires API key configuration
- REST-based communication

```java
@Component
public class OpenAIProvider implements AIProvider {
    @Value("${ai.openai.api-key:}")
    private String apiKey;
    
    @Value("${ai.openai.model:gpt-4}")
    private String model;
    
    @Override
    public boolean isAvailable() { 
        return apiKey != null && !apiKey.trim().isEmpty(); 
    }
}
```

##### Anthropic Claude Provider (`providers/AnthropicProvider.java`)
- Uses Anthropic Messages API
- Default model: `claude-sonnet-4-20250514`
- Requires API key and enablement flag
- Conditional loading via `@ConditionalOnProperty`

```java
@Component
@ConditionalOnProperty(name = "ai.anthropic.enabled", havingValue = "true")
public class AnthropicProvider implements AIProvider {
    @Value("${ai.anthropic.api-key:}")
    private String apiKey;
    
    @Value("${ai.anthropic.model:claude-sonnet-4-20250514}")
    private String model;
}
```

#### Configuration

**application.properties:**

```properties
# Default AI Provider (GOOGLE_ADK, OPENAI, or ANTHROPIC)
ai.provider.default=GOOGLE_ADK

# OpenAI Configuration
ai.openai.api-key=your-openai-api-key
ai.openai.model=gpt-4
ai.openai.api-url=https://api.openai.com/v1/chat/completions

# Anthropic Configuration  
ai.anthropic.enabled=false
ai.anthropic.api-key=your-anthropic-api-key
ai.anthropic.model=claude-sonnet-4-20250514
ai.anthropic.api-url=https://api.anthropic.com/v1/messages
```

#### Provider Selection

**1. Default Provider (via configuration):**
```java
@Service
public class AIGeneratedTablesService {
    @Value("${ai.provider.default:GOOGLE_ADK}")
    private String defaultProviderName;
    
    public AIGeneratedTables generateTables(AIGeneratedTablesRequest request) {
        return generateTables(request, defaultProviderName);
    }
}
```

**2. Specific Provider (via parameter):**
```java
public AIGeneratedTables generateTables(
    AIGeneratedTablesRequest request, 
    String providerName
) {
    AIProvider provider = aiProviderFactory.getProvider(providerName);
    return provider.generateTables(request);
}
```

**3. Get Available Providers:**
```java
public List<String> getAvailableProviders() {
    return aiProviderFactory.getAvailableProviders();
}
```

#### Provider Availability

Providers are considered available based on:

| Provider | Availability Criteria |
|----------|----------------------|
| Google ADK | Always (built-in agent) |
| OpenAI | API key is configured |
| Anthropic | Enabled flag + API key configured |

#### Example Usage

**Use Default Provider:**
```bash
curl -X POST http://localhost:8080/api/ai/generateTables \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Create users and orders tables",
    "currentTables": []
  }'
```

**Use Specific Provider (if supported in controller):**
```bash
curl -X POST http://localhost:8080/api/ai/generateTables?provider=OPENAI \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Create users and orders tables",
    "currentTables": []
  }'
```

#### Benefits

✅ **Flexibility**: Switch AI providers without code changes  
✅ **Vendor Independence**: Not locked into a single AI vendor  
✅ **Extensibility**: Easy to add new providers  
✅ **Resilience**: Fallback to different providers  
✅ **Cost Optimization**: Choose based on pricing needs  
✅ **Feature Selection**: Use provider-specific capabilities

---

### Action Types

The AI can perform four types of schema modifications:

#### 1. CREATE
Creates new tables. Skips duplicates by name.

```json
{
  "type": "create",
  "tables": [...]
}
```

#### 2. EDIT
Modifies existing tables (updates columns, relationships).

```json
{
  "type": "edit",
  "tables": [...]
}
```

#### 3. DELETE
Removes tables by name. Requires `allowDestructive: true`.

```json
{
  "type": "delete",
  "tableNames": ["old_table", "temp_table"]
}
```

#### 4. REPLACE
Completely replaces the entire schema. Requires `allowDestructive: true`.

```json
{
  "type": "replace",
  "newSchema": [...]
}
```

### Action Types

### LLM Agent Configuration (Google ADK)

The `GoogleADKProvider` configures a specialized LLM agent with detailed instructions:

**Model**: `gemini-2.0-flash`

**Instruction Highlights**:
- Output **only** valid JSON matching `AIGeneratedTables` structure
- Never nest action types (use `"type": "create"` not `{"create": {...}}`)
- Proper relationship detection:
  - ONE_TO_MANY: sourceTable has many targetTable records
  - MANY_TO_ONE: many sourceTable records reference one targetTable
  - MANY_TO_MANY: join table with exactly 2 foreign keys
- Foreign key constraints:
  - Set `foreignKey: true`, `referencedTable`, `referencedColumn`
  - Set `nullable` based on relationship optionality
- Java type mapping:
  - VARCHAR/TEXT → String
  - INT/INTEGER → Integer
  - BIGINT → Long
  - DECIMAL → BigDecimal
  - BOOLEAN → Boolean
  - DATE → LocalDate
  - TIMESTAMP → LocalDateTime

### Session Management

Sessions enable **conversation mode** where context is maintained across multiple AI requests:

- **In-memory session storage**: `ConcurrentHashMap<String, List<Table>>`
- **Session lifecycle**: Created on first request, reused for subsequent requests
- **Context preservation**: Current schema state is tracked per session
- **Max tables limit**: 50 tables per session (safety constraint)

### Example Usage

#### Simple Schema Creation
```bash
curl -X POST http://localhost:8080/api/ai/generateTables \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Create a blog schema with posts, comments, and tags",
    "currentTables": [],
    "allowDestructive": false
  }'
```

#### Modifying Existing Schema
```bash
curl -X POST http://localhost:8080/api/ai/generateTables \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Add created_at and updated_at timestamps to all tables",
    "currentTables": [...existing tables...],
    "sessionId": "session-abc123",
    "allowDestructive": false
  }'
```

### Error Handling

The service includes comprehensive error handling:

1. **Serialization errors**: JSON parsing failures
2. **ADK runtime errors**: Agent execution failures
3. **Invalid JSON**: Attempts to extract JSON from LLM output
4. **Action validation**: Validates action types and required fields
5. **Table limits**: Enforces MAX_TABLES constraint
6. **Fallback messages**: Provides helpful error descriptions

### Integration with Project Generation

AI-generated tables seamlessly integrate with the existing generation workflow:

1. User requests AI generation via frontend
2. Backend processes with LLM agent
3. Returns structured `Table[]` objects
4. Frontend normalizes tables (adds `id`, `position`)
5. Tables added to visual schema editor
6. User proceeds to project generation phase

---

## Contributing

### Code Style
- Follow Java naming conventions
- Use meaningful variable names
- Add JavaDoc comments for public methods
- Keep methods focused and small (< 50 lines)

### Git Workflow
1. Create feature branch: `git checkout -b feature/my-feature`
2. Make changes and commit: `git commit -m "Add feature"`
3. Push to remote: `git push origin feature/my-feature`
4. Create pull request

### Documentation
- Update this file when adding new features
- Add inline comments for complex logic
- Update JavaDoc for API changes

---

## Resources

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Initializr API](https://github.com/spring-io/initializr)
- [FreeMarker Documentation](https://freemarker.apache.org/docs/)
- [JPA Relationships Guide](https://www.baeldung.com/jpa-hibernate-associations)
- [Google Agent Development Kit](https://github.com/google/adk)
- [Gemini API Documentation](https://ai.google.dev/docs)

---

## License

[Add your license information here]

## Contact

For questions or support, contact: [Your contact information]

---

**Last Updated**: 2025-12-07
**Version**: 2.1
