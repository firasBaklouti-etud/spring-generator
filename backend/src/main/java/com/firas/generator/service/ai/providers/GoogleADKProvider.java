package com.firas.generator.service.ai.providers;

import com.fasterxml.jackson.core.type.TypeReference;
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
import dev.toonformat.jtoon.JToon;
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
                .model("gemini-2.5-flash")
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
        return "You are an SQL schema assistant that outputs ONLY valid TOON format matching the AIGeneratedTables structure.\n"
                + "INPUT: A user prompt and currentTables (array of Table objects) serialized in TOON.\n"
                + "OUTPUT: A TOON object with EXACTLY these three fields:\n"
                + "1. sessionId: optional string (can be empty or null)\n"
                + "2. actions: array of TableAction objects\n"
                + "3. explanation: string describing what was done\n\n"
                + "IMPORTANT: Your output MUST be in pure TOON format, NOT JSON and NOT markdown.\n"
                + "Do NOT wrap your output in ```json or ```toon or any markdown code blocks.\n"
                + "Do NOT output JSON format at all - only TOON format.\n"
                + "Do NOT include any explanatory text before or after the TOON output.\n"
                + "The entire response should be the TOON object only.\n\n"
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
                + "1. Output ONLY the TOON object, no additional text, no markdown, no code fences\n"
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
                + "IMPORTANT: Here is an example of the TOON output format you MUST follow perfectly:\n\n"
                + "sessionId: \"\"\n"
                + "actions[3]:\n"
                + "  - type: create\n"
                + "    tables[7]:\n"
                + "      - name: product_categories\n"
                + "        className: ProductCategory\n"
                + "        columns[2]{name,type,fieldName,javaType,primaryKey,autoIncrement,nullable,foreignKey,referencedTable,referencedColumn,unique}:\n"
                + "          id,BIGINT,id,Long,true,true,false,false,null,null,true\n"
                + "          name,VARCHAR(255),name,String,false,false,false,false,null,null,true\n"
                + "        relationships[0]:\n"
                + "        joinTable: false\n"
                + "      - name: payments\n"
                + "        className: Payment\n"
                + "        columns[4]{name,type,fieldName,javaType,primaryKey,autoIncrement,nullable,foreignKey,referencedTable,referencedColumn,unique}:\n"
                + "          id,BIGINT,id,Long,true,true,false,false,null,null,true\n"
                + "          order_id,BIGINT,orderId,Long,false,false,false,true,orders,id,false\n"
                + "          amount,\"DECIMAL(10,2)\",amount,BigDecimal,false,false,false,false,null,null,false\n"
                + "          payment_date,TIMESTAMP,paymentDate,java.time.Instant,false,false,false,false,null,null,false\n"
                + "        relationships[1]{type,sourceTable,targetTable,sourceColumn,targetColumn,joinTable,mappedBy,fieldName,targetClassName}:\n"
                + "          MANY_TO_ONE,payments,orders,order_id,id,null,null,order,Order\n"
                + "        joinTable: false\n"
                + "      - name: reviews\n"
                + "        className: Review\n"
                + "        columns[5]{name,type,fieldName,javaType,primaryKey,autoIncrement,nullable,foreignKey,referencedTable,referencedColumn,unique}:\n"
                + "          id,BIGINT,id,Long,true,true,false,false,null,null,true\n"
                + "          user_id,BIGINT,userId,Long,false,false,false,true,users,id,false\n"
                + "          product_id,BIGINT,productId,Long,false,false,false,false,null,null,false\n"
                + "          rating,INT,rating,Integer,false,false,false,false,null,null,false\n"
                + "          comment,TEXT,comment,String,false,false,true,false,null,null,false\n"
                + "        relationships[1]{type,sourceTable,targetTable,sourceColumn,targetColumn,joinTable,mappedBy,fieldName,targetClassName}:\n"
                + "          MANY_TO_ONE,reviews,users,user_id,id,null,null,user,User\n"
                + "        joinTable: false\n"
                + "      - name: cart\n"
                + "        className: Cart\n"
                + "        columns[4]{name,type,fieldName,javaType,primaryKey,autoIncrement,nullable,foreignKey,referencedTable,referencedColumn,unique}:\n"
                + "          id,BIGINT,id,Long,true,true,false,false,null,null,true\n"
                + "          user_id,BIGINT,userId,Long,false,false,false,true,users,id,false\n"
                + "          product_id,BIGINT,productId,Long,false,false,false,false,null,null,false\n"
                + "          quantity,INT,quantity,Integer,false,false,false,false,null,null,false\n"
                + "        relationships[1]{type,sourceTable,targetTable,sourceColumn,targetColumn,joinTable,mappedBy,fieldName,targetClassName}:\n"
                + "          MANY_TO_ONE,cart,users,user_id,id,null,null,user,User\n"
                + "        joinTable: false\n"
                + "      - name: address\n"
                + "        className: Address\n"
                + "        columns[6]{name,type,fieldName,javaType,primaryKey,autoIncrement,nullable,foreignKey,referencedTable,referencedColumn,unique}:\n"
                + "          id,BIGINT,id,Long,true,true,false,false,null,null,true\n"
                + "          user_id,BIGINT,userId,Long,false,false,false,true,users,id,false\n"
                + "          street,VARCHAR(255),street,String,false,false,false,false,null,null,false\n"
                + "          city,VARCHAR(255),city,String,false,false,false,false,null,null,false\n"
                + "          state,VARCHAR(255),state,String,false,false,false,false,null,null,false\n"
                + "          zip_code,VARCHAR(20),zipCode,String,false,false,false,false,null,null,false\n"
                + "        relationships[1]{type,sourceTable,targetTable,sourceColumn,targetColumn,joinTable,mappedBy,fieldName,targetClassName}:\n"
                + "          MANY_TO_ONE,address,users,user_id,id,null,null,user,User\n"
                + "        joinTable: false\n"
                + "      - name: coupons\n"
                + "        className: Coupon\n"
                + "        columns[3]{name,type,fieldName,javaType,primaryKey,autoIncrement,nullable,foreignKey,referencedTable,referencedColumn,unique}:\n"
                + "          id,BIGINT,id,Long,true,true,false,false,null,null,true\n"
                + "          code,VARCHAR(255),code,String,false,false,false,false,null,null,true\n"
                + "          discount,\"DECIMAL(10,2)\",discount,BigDecimal,false,false,false,false,null,null,false\n"
                + "        relationships[0]:\n"
                + "        joinTable: false\n"
                + "      - name: users_coupons\n"
                + "        className: UsersCoupons\n"
                + "        columns[2]{name,type,fieldName,javaType,primaryKey,autoIncrement,nullable,foreignKey,referencedTable,referencedColumn,unique}:\n"
                + "          user_id,BIGINT,userId,Long,true,false,false,true,users,id,false\n"
                + "          coupon_id,BIGINT,couponId,Long,true,false,false,true,coupons,id,false\n"
                + "        relationships[2]{type,sourceTable,targetTable,sourceColumn,targetColumn,joinTable,mappedBy,fieldName,targetClassName}:\n"
                + "          MANY_TO_ONE,users_coupons,users,user_id,id,null,null,user,User\n"
                + "          MANY_TO_ONE,users_coupons,coupons,coupon_id,id,null,null,coupon,Coupon\n"
                + "        joinTable: true\n"
                + "    tableNames: null\n"
                + "    newSchema: null\n"
                + "  - type: delete\n"
                + "    tables: null\n"
                + "    tableNames[1]: sessions\n"
                + "    newSchema: null\n"
                + "  - type: edit\n"
                + "    tables[1]:\n"
                + "      - name: orders\n"
                + "        className: Order\n"
                + "        columns[5]{name,type,fieldName,javaType,primaryKey,autoIncrement,nullable,foreignKey,referencedTable,referencedColumn,unique}:\n"
                + "          id,BIGINT,id,Long,true,true,false,false,null,null,false\n"
                + "          user_id,BIGINT,userId,Long,false,false,false,true,users,id,false\n"
                + "          total,\"DECIMAL(10,2)\",total,BigDecimal,false,false,false,false,null,null,false\n"
                + "          order_date,TIMESTAMP,orderDate,java.time.Instant,false,false,false,false,null,null,false\n"
                + "          shipping_address_id,BIGINT,shippingAddressId,Long,false,false,true,true,address,id,false\n"
                + "        relationships[2]{type,sourceTable,targetTable,sourceColumn,targetColumn,joinTable,mappedBy,fieldName,targetClassName}:\n"
                + "          MANY_TO_ONE,orders,users,user_id,id,null,null,user,User\n"
                + "          MANY_TO_ONE,orders,address,shipping_address_id,id,null,null,shippingAddress,Address\n"
                + "        joinTable: false\n"
                + "    tableNames: null\n"
                + "    newSchema: null\n"
                + "explanation: \"Added product categories, payment, reviews, cart, and address tables. Removed the sessions table. Introduced coupons with a many-to-many relation to users using a join table. Reordered and updated the orders table by adding shipping address and order date.\"\n";
    }

    private String buildPayload(String prompt, List<Table> currentTables) {
        try {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put("userRequest", prompt);

            // Convert List<Table> -> List<Map<String, Object>> using Jackson
            List<Map<String, Object>> currentTablesMap = objectMapper.convertValue(
                    currentTables,
                    new TypeReference<>() {}
            );
            inputMap.put("currentSchema", currentTablesMap);

            // Encode Map -> Toon string
            return JToon.encode(inputMap);

        } catch (Exception e) {
            throw new RuntimeException("Error serializing inputs to TOON: " + e.getMessage(), e);
        }
    }

    private AIGeneratedTables parseAIResponse(String assistantOutput, String sessionId) {
        try {
            // Decode Toon string -> Map/List structure
            Object decodedStructure = JToon.decode(assistantOutput);

            // Convert Map/List structure -> POJO using Jackson
            AIGeneratedTables aiResult = objectMapper.convertValue(
                    decodedStructure,
                    AIGeneratedTables.class
            );

            // Post-processing and fallback logic
            if (aiResult.getSessionId() != null && !aiResult.getSessionId().trim().isEmpty()) {
                sessionId = aiResult.getSessionId();
            }

            List<TableAction> actions = aiResult.getActions() != null ? aiResult.getActions() : new ArrayList<>();
            return new AIGeneratedTables(sessionId, actions, aiResult.getExplanation());

        } catch (Exception e) {
            // If TOON parsing fails, return an error result
            return new AIGeneratedTables(sessionId, Collections.emptyList(),
                    "Failed to parse AI TOON response. Raw output:\n" + assistantOutput + "\nParse error: " + e.getMessage());
        }
    }
}