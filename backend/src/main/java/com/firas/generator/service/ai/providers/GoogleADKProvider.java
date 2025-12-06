package com.firas.generator.service.ai.providers;

import com.firas.generator.model.*;
import com.firas.generator.model.AI.AIGeneratedTables;
import com.firas.generator.model.AI.AIGeneratedTablesRequest;
import com.firas.generator.model.AI.TableAction;
import com.firas.generator.service.ai.AIProvider;
import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LlmAgent;
import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.rxjava3.core.Flowable;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class GoogleADKProvider implements AIProvider {
    private static final String USER_ID = "student";
    private static final String NAME = "sql_table_assistent";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BaseAgent rootAgent;

    public GoogleADKProvider() {
        this.rootAgent = initAgent();
    }

    private BaseAgent initAgent() {
        String instruction = buildInstruction();

        return LlmAgent.builder()
                .name(NAME)
                .model("gemini-2.0-flash")
                .description("Agent to assist on SQL schema generation and modification.")
                .instruction(instruction)
                .build();
    }

    @Override
    public AIGeneratedTables generateTables(AIGeneratedTablesRequest request) {
        String prompt = Optional.ofNullable(request.getPrompt()).orElse("");
        List<Table> currentTables = request.getCurrentTables() != null ?
                new ArrayList<>(request.getCurrentTables()) : new ArrayList<>();
        String sessionId = request.getSessionId();

        if (sessionId == null) {
            sessionId = "session-" + UUID.randomUUID().toString().substring(0, 8);
        }

        String payload = buildPayload(prompt, currentTables);

        InMemoryRunner runner = new InMemoryRunner(rootAgent);
        Session session = runner.sessionService().createSession(NAME, USER_ID).blockingGet();
        Content userMsg = Content.fromParts(Part.fromText(payload));

        AtomicReference<String> finalAssistantText = new AtomicReference<>("");
        try {
            Flowable<Event> events = runner.runAsync(USER_ID, session.id(), userMsg);
            events.blockingForEach(event -> {
                if (event.finalResponse()) {
                    finalAssistantText.set(event.stringifyContent());
                }
            });
        } catch (Exception e) {
            return new AIGeneratedTables(sessionId, Collections.emptyList(),
                    "Error running Google ADK agent: " + e.getMessage());
        }

        String assistantOutput = finalAssistantText.get();
        if (assistantOutput == null || assistantOutput.trim().isEmpty()) {
            return new AIGeneratedTables(sessionId, Collections.emptyList(),
                    "AI produced no output.");
        }

        return parseAIResponse(assistantOutput, sessionId);
    }

    @Override
    public String getProviderName() {
        return "GOOGLE_ADK";
    }

    @Override
    public boolean isAvailable() {
        return rootAgent != null;
    }

    private String buildInstruction() {
        return "You are an SQL schema assistant that outputs ONLY valid JSON matching the AIGeneratedTables structure.\n"
                + "INPUT: A user prompt and currentTables (array of Table objects).\n"
                + "OUTPUT: A JSON object with EXACTLY these three fields:\n"
                + "1. sessionId: optional string (can be empty or null)\n"
                + "2. actions: array of TableAction objects\n"
                + "3. explanation: string describing what was done\n\n"
                + "TABLE ACTION STRUCTURE:\n"
                + "Each TableAction MUST have:\n"
                + "- type: string (must be one of: \"create\", \"edit\", \"delete\", \"replace\")\n"
                + "- ONE of the following based on type:\n"
                + "  * For \"create\" or \"edit\": include \"tables\" array with Table objects\n"
                + "  * For \"delete\": include \"tableNames\" array with string table names\n"
                + "  * For \"replace\": include \"newSchema\" array with Table objects\n\n"
                + "TABLE STRUCTURE:\n"
                + "{\n"
                + "  \"name\": \"table_name\",\n"
                + "  \"className\": \"ClassName\",\n"
                + "  \"columns\": [array of Column objects],\n"
                + "  \"relationships\": [array of Relationship objects],\n"
                + "  \"joinTable\": boolean\n"
                + "}\n\n"
                + "COLUMN STRUCTURE:\n"
                + "{\n"
                + "  \"name\": \"column_name\",\n"
                + "  \"type\": \"SQL_TYPE\",\n"
                + "  \"fieldName\": \"javaFieldName\",\n"
                + "  \"javaType\": \"JavaType\",\n"
                + "  \"primaryKey\": boolean,\n"
                + "  \"autoIncrement\": boolean,\n"
                + "  \"nullable\": boolean,\n"
                + "  \"foreignKey\": boolean,\n"
                + "  \"referencedTable\": \"string or null\",\n"
                + "  \"referencedColumn\": \"string or null\",\n"
                + "  \"unique\": boolean\n"
                + "}\n\n"
                + "RELATIONSHIP STRUCTURE:\n"
                + "{\n"
                + "  \"type\": \"ONE_TO_MANY|MANY_TO_ONE|ONE_TO_ONE|MANY_TO_MANY\",\n"
                + "  \"sourceTable\": \"table_name\",\n"
                + "  \"targetTable\": \"table_name\",\n"
                + "  \"sourceColumn\": \"column_name\",\n"
                + "  \"targetColumn\": \"column_name\",\n"
                + "  \"joinTable\": \"string or null\",\n"
                + "  \"mappedBy\": \"string or null\",\n"
                + "  \"fieldName\": \"javaFieldName\",\n"
                + "  \"targetClassName\": \"ClassName\"\n"
                + "}\n\n"
                + "CRITICAL RULES:\n"
                + "1. Output ONLY the JSON object, no additional text, no markdown, no code fences\n"
                + "2. NEVER nest action types like {\"create\": {...}}. Use {\"type\": \"create\", \"tables\": [...]}\n"
                + "3. For relationships:\n"
                + "   - ONE_TO_MANY: sourceTable has many targetTable records\n"
                + "   - MANY_TO_ONE: many sourceTable records reference one targetTable\n"
                + "   - For bidirectional relationships, use mappedBy appropriately\n"
                + "   - For MANY_TO_MANY, set joinTable to the join table name\n"
                + "4. For foreign keys:\n"
                + "   - Set foreignKey: true\n"
                + "   - Set referencedTable and referencedColumn\n"
                + "   - Set nullable based on relationship optionality\n"
                + "5. Primary key columns should have: primaryKey: true, autoIncrement: true (for IDs), unique: true\n"
                + "6. Java types must match SQL types:\n"
                + "   - VARCHAR/TEXT → String\n"
                + "   - INT/INTEGER → Integer\n"
                + "   - BIGINT → Long\n"
                + "   - DECIMAL/NUMERIC → java.math.BigDecimal\n"
                + "   - BOOLEAN → Boolean\n"
                + "   - DATE → java.time.LocalDate\n"
                + "   - TIMESTAMP → java.time.Instant or java.time.LocalDateTime\n"
                + "7. Field names should be camelCase versions of column names\n"
                + "\n"
                + "IMPORTANT: Return ONLY the JSON object. No extra text before or after.";
    }

    private String buildPayload(String prompt, List<Table> currentTables) {
        try {
            String currentTablesJson = objectMapper.writeValueAsString(currentTables);
            return "USER REQUEST: " + prompt + "\n\n"
                    + "CURRENT SCHEMA: " + currentTablesJson + "\n\n"
                    + "INSTRUCTIONS: Analyze the user request and current schema. Return a JSON response "
                    + "in the exact AIGeneratedTables format described above. "
                    + "Apply appropriate schema changes. "
                    + "For destructive operations (delete, replace), ensure they're appropriate. "
                    + "Use sessionId to maintain conversation context if needed.\n\n"
                    + "RESPONSE FORMAT REQUIRED: Pure JSON matching AIGeneratedTables structure.";
        } catch (Exception e) {
            throw new RuntimeException("Error serializing current tables: " + e.getMessage(), e);
        }
    }

    private AIGeneratedTables parseAIResponse(String assistantOutput, String sessionId) {
        AIGeneratedTables aiResult;
        try {
            aiResult = objectMapper.readValue(assistantOutput, AIGeneratedTables.class);
        } catch (Exception e) {
            try {
                int first = assistantOutput.indexOf('{');
                int last = assistantOutput.lastIndexOf('}');
                if (first >= 0 && last > first) {
                    String json = assistantOutput.substring(first, last + 1);
                    aiResult = objectMapper.readValue(json, AIGeneratedTables.class);
                } else {
                    return new AIGeneratedTables(sessionId, Collections.emptyList(),
                            "AI did not return valid JSON. Raw output:\n" + assistantOutput);
                }
            } catch (Exception ex2) {
                return new AIGeneratedTables(sessionId, Collections.emptyList(),
                        "Failed to parse AI JSON. Raw output:\n" + assistantOutput + "\nParse error: " + ex2.getMessage());
            }
        }

        if (aiResult.getSessionId() != null && !aiResult.getSessionId().trim().isEmpty()) {
            sessionId = aiResult.getSessionId();
        }

        List<TableAction> actions = aiResult.getActions() != null ? aiResult.getActions() : new ArrayList<>();
        return new AIGeneratedTables(sessionId, actions, aiResult.getExplanation());
    }
}