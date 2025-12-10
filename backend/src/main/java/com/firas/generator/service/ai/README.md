# AI Generation Service

The **AI Service** enables users to generate complete SQL schemas from natural language descriptions (e.g., "Create a school management system"). It abstracts the underlying LLM providers (OpenAI, Anthropic, Google Gemini) to provide a unified interface.

## 🧠 Core Concept

The service translates a user's text prompt into a structured JSON representation of database tables, which is then converted into the application's internal `Table` model.

**Prompt Engineering**: We use a specialized "System Prompt" that forces the LLM to output ONLY valid JSON matching our specific schema structure, ensuring the result is machine-readable without valid parsing.

## 🏗️ Architecture

We use the **Provider Pattern** to support multiple AI backends easily.

```mermaid
classDiagram
    class AIController {
        +generateTables(prompt)
    }
    
    class AIGeneratedTablesService {
        +generateTables(prompt)
    }
    
    class AIProvider {
        <<interface>>
        +generate(prompt) String
    }
    
    class OpenAIProvider {
        +generate(prompt)
    }
    
    class AnthropicProvider {
        +generate(prompt)
    }
    
    class GoogleADKProvider {
        +generate(prompt)
    }
    
    AIController --> AIGeneratedTablesService
    AIGeneratedTablesService --> AIProvider
    AIProvider <|.. OpenAIProvider
    AIProvider <|.. AnthropicProvider
    AIProvider <|.. GoogleADKProvider
```

## 🔄 Data Flow

```mermaid
sequenceDiagram
    participant User
    participant Controller
    participant Service as AIGeneratedTablesService
    participant Provider as AIProvider
    participant LLM as External AI API

    User->>Controller: POST /api/ai/generateTables "E-commerce app"
    Controller->>Service: generateTables("E-commerce app")
    
    note over Service: 1. Construct System Prompt
    Service->>Service: Load prompt_template.txt
    
    note over Service: 2. Call AI Provider
    Service->>Provider: generate(full_prompt)
    Provider->>LLM: API Request
    LLM-->>Provider: JSON String
    Provider-->>Service: JSON String
    
    note over Service: 3. Parse & Validate
    Service->>Service: ObjectMapper.readValue(json)
    
    Service-->>Controller: List<Table>
    Controller-->>User: JSON Response (Tables)
```

## 📝 Configuration

AI providers are configured via `application.properties`. API keys should be set as environment variables for security.

```properties
# Select active provider
app.ai.provider=openai  # openai | anthropic | google

# API Keys
spring.ai.openai.api-key=${OPENAI_API_KEY}
spring.ai.anthropic.api-key=${ANTHROPIC_API_KEY}
google.ai.api-key=${GOOGLE_API_KEY}
```

## 🤖 System Prompt Strategy

The system prompt is critical. It instructs the AI to:
1.  Act as a Senior Database Architect.
2.  Follow strict JSON format.
3.  Include `className`, `columns` with correct SQL types, and `relationships`.
4.  **NEVER** include markdown formatting (like \`\`\`json) in the response, just raw JSON.

**Example JSON Response Expected:**
```json
[
  {
    "name": "users",
    "columns": [
      {"name": "id", "type": "BIGINT", "primaryKey": true},
      {"name": "email", "type": "VARCHAR"}
    ]
  }
]
```
