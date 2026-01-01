package com.firas.generator.model.config;

public class SecurityRule {
    private String path;
    private String method; // GET, POST, PUT, DELETE, ALL
    private String rule; // PERMIT_ALL, AUTHENTICATED, HAS_ROLE
    private String role; // e.g. "ADMIN", "USER" (only if rule is HAS_ROLE)

    // Getters and Setters
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getRule() { return rule; }
    public void setRule(String rule) { this.rule = rule; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
