package com.firas.generator.service.ai.providers;

import com.firas.generator.model.*;
import com.firas.generator.model.AI.AIGeneratedTables;
import com.firas.generator.model.AI.AIGeneratedTablesRequest;
import com.firas.generator.service.ai.AIProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.*;

@Component
public class OpenAIProvider implements AIProvider {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ai.openai.api-key:}")
    private String apiKey;

    @Value("${ai.openai.model:gpt-4}")
    private String model;

    @Value("${ai.openai.api-url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;

    @Override
    public AIGeneratedTables generateTables(AIGeneratedTablesRequest request) {
        String prompt = Optional.ofNullable(request.getPrompt()).orElse("");
        List<Table> currentTables = request.getCurrentTables() != null ?
                new ArrayList<>(request.getCurrentTables()) : new ArrayList<>();
        String sessionId = request.getSessionId();

        if (sessionId == null) {
            sessionId = "session-" + UUID.randomUUID().toString().substring(0, 8);
        }

        try {
            String systemPrompt = buildSystemPrompt();
            String userPrompt = buildUserPrompt(prompt, currentTables);

            // Build OpenAI request
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", Arrays.asList(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
            ));
            requestBody.put("temperature", 0.7);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    String content = (String) message.get("content");

                    return parseAIResponse(content, sessionId);
                }
            }

            return new AIGeneratedTables(sessionId, Collections.emptyList(),
                    "OpenAI API returned no valid response");

        } catch (Exception e) {
            return new AIGeneratedTables(sessionId, Collections.emptyList(),
                    "Error calling OpenAI API: " + e.getMessage());
        }
    }

    @Override
    public String getProviderName() {
        return "OPENAI";
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    private String buildSystemPrompt() {
        return "You are an SQL schema assistant that outputs ONLY valid JSON matching the AIGeneratedTables structure. "
                + "Return a JSON object with: sessionId (string), actions (array), explanation (string). "
                + "Each action has type (create/edit/delete/replace) and corresponding data. "
                + "Output ONLY JSON, no markdown, no code fences.";
    }

    private String buildUserPrompt(String prompt, List<Table> currentTables) {
        try {
            String currentTablesJson = objectMapper.writeValueAsString(currentTables);
            return "USER REQUEST: " + prompt + "\n\nCURRENT SCHEMA: " + currentTablesJson;
        } catch (Exception e) {
            throw new RuntimeException("Error serializing tables", e);
        }
    }

    private AIGeneratedTables parseAIResponse(String content, String sessionId) {
        try {
            // Try direct parse
            return objectMapper.readValue(content, AIGeneratedTables.class);
        } catch (Exception e) {
            // Try extracting JSON
            try {
                int first = content.indexOf('{');
                int last = content.lastIndexOf('}');
                if (first >= 0 && last > first) {
                    String json = content.substring(first, last + 1);
                    return objectMapper.readValue(json, AIGeneratedTables.class);
                }
            } catch (Exception ex) {
                // Fallback
            }
            return new AIGeneratedTables(sessionId, Collections.emptyList(),
                    "Failed to parse OpenAI response: " + content);
        }
    }
}

