package com.firas.generator.model.AI;

import java.util.List;

public class AIGeneratedTables {
    private String sessionId;
    private List<TableAction> actions;
    private String explanation;

    public AIGeneratedTables(String sessionId, List<TableAction>  actions, String explanation) {
        this.sessionId = sessionId;
        this.actions = actions;
        this.explanation = explanation;
    }

    public AIGeneratedTables() {
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public List<TableAction>  getActions() {
        return actions;
    }

    public void setActions(List<TableAction>  actions) {
        this.actions = actions;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    @Override
    public String   toString() {
        return "AIGeneratedTables{" +
                "sessionId='" + sessionId + '\'' +
                ", actions=" + actions +
                ", explanation='" + explanation + '\'' +
                '}';
    }
}
