package com.firas.generator.service;

import com.firas.generator.model.*;
import com.firas.generator.model.AI.AIGeneratedTables;
import com.firas.generator.model.AI.AIGeneratedTablesRequest;
import com.firas.generator.model.AI.TableAction;
import com.firas.generator.service.ai.AIProviderFactory;
import com.firas.generator.service.ai.AIProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AIGeneratedTablesService {
    private static final int MAX_TABLES = 50;

    private final AIProviderFactory aiProviderFactory;
    private final Map<String, List<Table>> sessionTables = new ConcurrentHashMap<>();

    @Value("${ai.provider.default:GOOGLE_ADK}")
    private String defaultProviderName;

    @Autowired
    public AIGeneratedTablesService(AIProviderFactory aiProviderFactory) {
        this.aiProviderFactory = aiProviderFactory;
    }

    /**
     * Generate tables using the default AI provider
     */
    public AIGeneratedTables generateTables(AIGeneratedTablesRequest request) {
        return generateTables(request, defaultProviderName);
    }

    /**
     * Generate tables using a specific AI provider
     */
    public AIGeneratedTables generateTables(AIGeneratedTablesRequest request, String providerName) {
        AIProvider provider = aiProviderFactory.getProvider(providerName);

        String sessionId = request.getSessionId();
        List<Table> currentTables = request.getCurrentTables() != null ?
                new ArrayList<>(request.getCurrentTables()) : new ArrayList<>();

        // Merge with session context if available
        if (sessionId != null && sessionTables.containsKey(sessionId)) {
            currentTables = new ArrayList<>(sessionTables.get(sessionId));
            request.setCurrentTables(currentTables);
        } else if (sessionId == null) {
            sessionId = "session-" + UUID.randomUUID().toString().substring(0, 8);
            request.setSessionId(sessionId);
        }

        // Delegate to the AI provider
        AIGeneratedTables result = provider.generateTables(request);

        // Update session context
        if (result.getSessionId() != null && !result.getSessionId().trim().isEmpty()) {
            sessionId = result.getSessionId();
        }

        try {
            updateSessionTables(sessionId, result.getActions(), currentTables);
        } catch (Exception e) {
            return new AIGeneratedTables(sessionId, result.getActions(),
                    result.getExplanation() + "\n\nError applying actions: " + e.getMessage());
        }

        return new AIGeneratedTables(sessionId, result.getActions(), result.getExplanation());
    }

    /**
     * Get list of available AI providers
     */
    public List<String> getAvailableProviders() {
        return aiProviderFactory.getAvailableProviders();
    }

    private void updateSessionTables(String sessionId, List<TableAction> actions, List<Table> currentTables) {
        if (sessionId == null) {
            sessionId = "session-" + UUID.randomUUID().toString().substring(0, 8);
        }

        List<Table> working = new ArrayList<>(currentTables != null ? currentTables : Collections.emptyList());

        for (TableAction action : actions) {
            if (action == null || action.getType() == null) continue;
            String type = action.getType().toString().toLowerCase(Locale.ROOT).trim();

            switch (type) {
                case "create":
                    if (action.getTables() != null) {
                        for (Table t : action.getTables()) {
                            boolean exists = working.stream().anyMatch(ex -> ex.getName().equalsIgnoreCase(t.getName()));
                            if (!exists) {
                                working.add(t);
                            }
                        }
                    }
                    break;

                case "edit":
                    if (action.getTables() != null) {
                        for (Table updated : action.getTables()) {
                            int idx = -1;
                            for (int i = 0; i < working.size(); i++) {
                                if (working.get(i).getName().equalsIgnoreCase(updated.getName())) {
                                    idx = i;
                                    break;
                                }
                            }
                            if (idx != -1) {
                                Table existing = working.get(idx);
                                if (updated.getRelationships() == null) {
                                    updated.setRelationships(existing.getRelationships());
                                }
                                working.set(idx, updated);
                            } else {
                                working.add(updated);
                            }
                        }
                    }
                    break;

                case "delete":
                    if (action.getTableNames() != null) {
                        for (String name : action.getTableNames()) {
                            working.removeIf(t -> t.getName().equalsIgnoreCase(name));
                        }
                    }
                    break;

                case "replace":
                    if (action.getNewSchema() != null) {
                        working = new ArrayList<>(action.getNewSchema());
                    }
                    break;

                default:
                    break;
            }

            if (working.size() > MAX_TABLES) {
                working = new ArrayList<>(working.subList(0, MAX_TABLES));
            }
        }

        sessionTables.put(sessionId, working);
    }
}