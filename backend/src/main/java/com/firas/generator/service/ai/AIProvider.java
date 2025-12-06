// 1. AIProvider Interface
package com.firas.generator.service.ai;

import com.firas.generator.model.AI.AIGeneratedTables;
import com.firas.generator.model.AI.AIGeneratedTablesRequest;

public interface AIProvider {
    /**
     * Generate tables based on the request using the specific AI provider
     */
    AIGeneratedTables generateTables(AIGeneratedTablesRequest request);

    /**
     * Get the provider name
     */
    String getProviderName();

    /**
     * Check if the provider is available/configured
     */
    boolean isAvailable();
}