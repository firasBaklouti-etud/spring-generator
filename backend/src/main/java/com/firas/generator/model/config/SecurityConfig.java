package com.firas.generator.model.config;

import java.util.List;
import java.util.Map;

public class SecurityConfig {
    private boolean enabled;
    private String authenticationType; // "BASIC", "JWT", "OAUTH2"
    private boolean useDbAuth; // If true, generate User/Role entities
    private List<String> oauth2Providers; // e.g. "google", "github"

    // Advanced Security Config
    private String principalEntity; // e.g. "User"
    private String usernameField;   // e.g. "email"
    private String passwordField;   // e.g. "password"
    private String roleStrategy;    // "STRING" or "ENTITY" (Legacy)
    private String roleEntity;      // e.g. "Role" (if strategy is ENTITY)
    
    // Dual-Mode RBAC Configuration
    private String rbacMode;        // "STATIC" or "DYNAMIC"
    private List<String> permissions; // List of permission strings (e.g. "USER_READ")
    private List<RoleDefinition> definedRoles; // Role definitions with permissions

    private List<SecurityRule> rules;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getAuthenticationType() {
        return authenticationType;
    }

    public void setAuthenticationType(String authenticationType) {
        this.authenticationType = authenticationType;
    }

    public boolean isUseDbAuth() {
        return useDbAuth;
    }

    public void setUseDbAuth(boolean useDbAuth) {
        this.useDbAuth = useDbAuth;
    }

    public List<String> getOauth2Providers() {
        return oauth2Providers;
    }

    public void setOauth2Providers(List<String> oauth2Providers) {
        this.oauth2Providers = oauth2Providers;
    }

    public String getPrincipalEntity() {
        return principalEntity;
    }

    public void setPrincipalEntity(String principalEntity) {
        this.principalEntity = principalEntity;
    }

    public String getUsernameField() {
        return usernameField;
    }

    public void setUsernameField(String usernameField) {
        this.usernameField = usernameField;
    }

    public String getPasswordField() {
        return passwordField;
    }

    public void setPasswordField(String passwordField) {
        this.passwordField = passwordField;
    }

    public String getRoleStrategy() {
        return roleStrategy;
    }

    public void setRoleStrategy(String roleStrategy) {
        this.roleStrategy = roleStrategy;
    }

    public String getRoleEntity() {
        return roleEntity;
    }

    public void setRoleEntity(String roleEntity) {
        this.roleEntity = roleEntity;
    }

    public List<SecurityRule> getRules() {
        return rules;
    }

    public void setRules(List<SecurityRule> rules) {
        this.rules = rules;
    }

    public String getRbacMode() {
        return rbacMode;
    }

    public void setRbacMode(String rbacMode) {
        this.rbacMode = rbacMode;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    public List<RoleDefinition> getDefinedRoles() {
        return definedRoles;
    }

    public void setDefinedRoles(List<RoleDefinition> definedRoles) {
        this.definedRoles = definedRoles;
    }

    // Inner class for role definitions
    public static class RoleDefinition {
        private String name;
        private List<String> permissions;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<String> getPermissions() {
            return permissions;
        }

        public void setPermissions(List<String> permissions) {
            this.permissions = permissions;
        }
    }
}
