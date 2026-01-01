package com.firas.generator.model.AI;

import com.firas.generator.model.Table;

import java.util.List;

public class AIGeneratedTablesRequest {
    private String prompt;
    private List<Table> currentTables;
    private String sessionId;
    private boolean allowDestructive;

    public AIGeneratedTablesRequest(String prompt, List<Table> currentTables, String sessionId, boolean allowDestructive) {
        this.prompt = prompt;
        this.currentTables = currentTables;
        this.sessionId = sessionId;
        this.allowDestructive = allowDestructive;
    }

    public AIGeneratedTablesRequest() {

    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public List<Table> getCurrentTables() {
        return currentTables;
    }

    public void setCurrentTables(List<Table> currentTables) {
        this.currentTables = currentTables;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public boolean isAllowDestructive() {
        return allowDestructive;
    }

    public void setAllowDestructive(boolean allowDestructive) {
        this.allowDestructive = allowDestructive;
    }


    @Override
    public String toString() {
        return "AIGeneratedTablesRequest{" +
                "prompt='" + prompt + '\'' +
                ", currentTables=" + currentTables +
                ", sessionId='" + sessionId + '\'' +
                ", allowDestructive=" + allowDestructive +
                '}';
    }
}
