package com.firas.generator.service.ai.providers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.firas.generator.model.AI.AIGeneratedTables;
import com.firas.generator.model.AI.TableAction;
import com.firas.generator.model.AI.TableActionType;
import com.firas.generator.model.Relationship;
import com.firas.generator.model.RelationshipType;
import com.firas.generator.model.Table;
import com.firas.generator.model.Column;
import dev.toonformat.jtoon.JToon;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

public class ToonSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void shouldPreserveNullValuesDuringToonRoundTrip() throws Exception {
        // Arrange: construct domain model
        Table sourceTable = new Table();
        sourceTable.setName("users");
        sourceTable.setClassName("User");

        Column idColumn = new Column();
        idColumn.setName("id");
        idColumn.setType("BIGINT");
        sourceTable.addColumn(idColumn);

        // Serialize: POJO -> Map -> TOON
        Map<String, Object> sourceMap =
                mapper.convertValue(sourceTable, new TypeReference<>() {});
        String toonEncodedInput = JToon.encode(sourceMap);

        // Deserialize: TOON -> Map -> POJO
        Object toonDecodedStructure = JToon.decode(toonEncodedInput);
        Table deserializedTable = mapper.convertValue(toonDecodedStructure, Table.class);

        // Validate: null fields remain null
        Column deserializedIdColumn = deserializedTable.getColumns().get(0);
        assertNull(deserializedIdColumn.getFieldName(), "fieldName should remain null");
        assertNull(deserializedIdColumn.getJavaType(), "javaType should remain null");
        assertNull(deserializedIdColumn.getReferencedTable(), "referencedTable should remain null");
        assertNull(deserializedIdColumn.getReferencedColumn(), "referencedColumn should remain null");

        // Round-trip re-encoding
        Map<String, Object> deserializedMap =
                mapper.convertValue(deserializedTable, new TypeReference<>() {});
        String toonEncodedOutput = JToon.encode(deserializedMap);

        // Assert semantic equality by comparing decoded structures
        assertEquals(
                JToon.decode(toonEncodedInput),
                JToon.decode(toonEncodedOutput),
                "TOON round-trip must preserve structure equivalence"
        );

        System.out.println("TOON input:");
        System.out.println(toonEncodedInput);
        System.out.println("TOON output:");
        System.out.println(toonEncodedOutput);
    }


    @Test
    public void shouldSerializeAndDeserializeComplexAIGeneratedTablesStructure() throws Exception {

        // ============================================================
        // Arrange: Build a complex, realistic AIGeneratedTables object
        // ============================================================

        AIGeneratedTables aiPayload = new AIGeneratedTables();
        aiPayload.setSessionId("session-123");
        aiPayload.setExplanation("Complex schema update");

        // ---- Table 1: users ----
        Table usersTable = new Table();
        usersTable.setName("users");
        usersTable.setClassName("User");

        Column userId = new Column();
        userId.setName("id");
        userId.setType("BIGINT");
        userId.setPrimaryKey(true);
        userId.setAutoIncrement(true);
        usersTable.addColumn(userId);

        Column username = new Column();
        username.setName("username");
        username.setType("VARCHAR");
        username.setNullable(false);
        username.setUnique(true);
        usersTable.addColumn(username);

        // Relationship (User -> Order) ONE_TO_MANY
        Relationship userOrdersRel = new Relationship();
        userOrdersRel.setType(RelationshipType.ONE_TO_MANY);
        userOrdersRel.setSourceTable("users");
        userOrdersRel.setTargetTable("orders");
        userOrdersRel.setSourceColumn("id");
        userOrdersRel.setTargetColumn("user_id");
        userOrdersRel.setFieldName("orders");
        userOrdersRel.setTargetClassName("Order");
        usersTable.getRelationships().add(userOrdersRel);

        // ---- Table 2: orders ----
        Table ordersTable = new Table();
        ordersTable.setName("orders");
        ordersTable.setClassName("Order");

        Column orderId = new Column();
        orderId.setName("id");
        orderId.setType("BIGINT");
        orderId.setPrimaryKey(true);
        orderId.setAutoIncrement(true);
        ordersTable.addColumn(orderId);

        Column orderUserId = new Column();
        orderUserId.setName("user_id");
        orderUserId.setType("BIGINT");
        orderUserId.setForeignKey(true);
        orderUserId.setReferencedTable("users");
        orderUserId.setReferencedColumn("id");
        ordersTable.addColumn(orderUserId);

        // Relationship (Order -> User) MANY_TO_ONE
        Relationship ordersUserRel = new Relationship();
        ordersUserRel.setType(RelationshipType.MANY_TO_ONE);
        ordersUserRel.setSourceTable("orders");
        ordersUserRel.setTargetTable("users");
        ordersUserRel.setSourceColumn("user_id");
        ordersUserRel.setTargetColumn("id");
        ordersUserRel.setFieldName("user");
        ordersUserRel.setTargetClassName("User");
        ordersTable.getRelationships().add(ordersUserRel);

        // ---- TableAction ----
        TableAction action = new TableAction();
        action.setType(TableActionType.edit);
        action.setTables(List.of(usersTable, ordersTable));
        action.setTableNames(List.of("users", "orders"));
        action.setNewSchema(List.of(usersTable, ordersTable));

        aiPayload.setActions(List.of(action));

        // ============================================================
        // Act: TOON round-trip
        // ============================================================

        Map<String, Object> initialMap =
                mapper.convertValue(aiPayload, new TypeReference<>() {});

        String toonEncodedInput = JToon.encode(initialMap);

        Object decodedStructure = JToon.decode(toonEncodedInput);

        AIGeneratedTables reconstructed =
                mapper.convertValue(decodedStructure, AIGeneratedTables.class);

        Map<String, Object> reconstructedMap =
                mapper.convertValue(reconstructed, new TypeReference<>() {});

        String toonEncodedOutput = JToon.encode(reconstructedMap);

        // ============================================================
        // Assert: semantic equivalence — structures must match
        // ============================================================

        assertEquals(
                JToon.decode(toonEncodedInput),
                JToon.decode(toonEncodedOutput),
                "TOON round-trip must preserve full nested structure"
        );

        // Additional strong validation checks
        assertEquals("session-123", reconstructed.getSessionId());
        assertEquals(1, reconstructed.getActions().size());

        TableAction reconstructedAction = reconstructed.getActions().get(0);
        assertEquals(2, reconstructedAction.getTables().size());
        assertEquals(2, reconstructedAction.getNewSchema().size());
        assertEquals(2, reconstructedAction.getTableNames().size());

        // Validate critical foreign key fields remain intact
        Table reconstructedOrders = reconstructedAction.getTables().get(1);
        Column reconstructedFk = reconstructedOrders.getColumns()
                .stream().filter(c -> "user_id".equals(c.getName()))
                .findFirst().orElseThrow();

        assertTrue(reconstructedFk.isForeignKey());
        assertEquals("users", reconstructedFk.getReferencedTable());
        assertEquals("id", reconstructedFk.getReferencedColumn());

        // Output for debugging (safe to keep)
        System.out.println("TOON input:\n" + toonEncodedInput);
        System.out.println("TOON output:\n" + toonEncodedOutput);
    }

}
