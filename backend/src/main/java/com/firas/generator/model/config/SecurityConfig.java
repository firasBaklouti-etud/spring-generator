package com.firas.generator.model.config;

import java.util.List;
import java.util.Map;

public class SecurityConfig {
    private boolean enabled;
    private String authenticationType; // "BASIC", "JWT", "OAUTH2", "FORM_LOGIN", "KEYCLOAK_RS", "KEYCLOAK_OAUTH"
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

    // JWT Configuration
    private String signingAlgorithm;        // "HS256" or "RS256"

    // Social Login
    private List<String> socialLogins;      // ["GOOGLE", "GITHUB", "FACEBOOK"]
    private Map<String, SocialProviderConfig> socialProviderConfigs; // per-provider client config

    // Keycloak Configuration
    private boolean keycloakEnabled;
    private String keycloakRealm;
    private String keycloakClientId;
    private String keycloakClientSecret;
    private String keycloakIssuerUrl;

    // Password Reset
    private boolean passwordResetEnabled;
    private String passwordResetTokenField;
    private String passwordResetExpiryField;

    // Refresh Token Persistence
    private boolean refreshTokenPersisted;  // DB vs in-memory (JWT-based)
    private String refreshTokenEntity;      // table name for persisted tokens

    // Remember-Me
    private boolean rememberMeEnabled;
    private String rememberMeMode;          // "ALWAYS" or "CHECKBOX"

    // Registration
    private boolean registrationEnabled = true;

    // Security Code Style
    private String securityStyle;           // "ANNOTATION" or "CONFIG"

    // Fallback & Testing
    private boolean staticUserFallback;     // in-memory users when no user table
    private boolean testUsersEnabled;

    // ======== Getters & Setters ========

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

    public String getSigningAlgorithm() {
        return signingAlgorithm;
    }

    public void setSigningAlgorithm(String signingAlgorithm) {
        this.signingAlgorithm = signingAlgorithm;
    }

    public List<String> getSocialLogins() {
        return socialLogins;
    }

    public void setSocialLogins(List<String> socialLogins) {
        this.socialLogins = socialLogins;
    }

    public Map<String, SocialProviderConfig> getSocialProviderConfigs() {
        return socialProviderConfigs;
    }

    public void setSocialProviderConfigs(Map<String, SocialProviderConfig> socialProviderConfigs) {
        this.socialProviderConfigs = socialProviderConfigs;
    }

    public boolean isKeycloakEnabled() {
        return keycloakEnabled;
    }

    public void setKeycloakEnabled(boolean keycloakEnabled) {
        this.keycloakEnabled = keycloakEnabled;
    }

    public String getKeycloakRealm() {
        return keycloakRealm;
    }

    public void setKeycloakRealm(String keycloakRealm) {
        this.keycloakRealm = keycloakRealm;
    }

    public String getKeycloakClientId() {
        return keycloakClientId;
    }

    public void setKeycloakClientId(String keycloakClientId) {
        this.keycloakClientId = keycloakClientId;
    }

    public String getKeycloakClientSecret() {
        return keycloakClientSecret;
    }

    public void setKeycloakClientSecret(String keycloakClientSecret) {
        this.keycloakClientSecret = keycloakClientSecret;
    }

    public String getKeycloakIssuerUrl() {
        return keycloakIssuerUrl;
    }

    public void setKeycloakIssuerUrl(String keycloakIssuerUrl) {
        this.keycloakIssuerUrl = keycloakIssuerUrl;
    }

    public boolean isPasswordResetEnabled() {
        return passwordResetEnabled;
    }

    public void setPasswordResetEnabled(boolean passwordResetEnabled) {
        this.passwordResetEnabled = passwordResetEnabled;
    }

    public String getPasswordResetTokenField() {
        return passwordResetTokenField;
    }

    public void setPasswordResetTokenField(String passwordResetTokenField) {
        this.passwordResetTokenField = passwordResetTokenField;
    }

    public String getPasswordResetExpiryField() {
        return passwordResetExpiryField;
    }

    public void setPasswordResetExpiryField(String passwordResetExpiryField) {
        this.passwordResetExpiryField = passwordResetExpiryField;
    }

    public boolean isRefreshTokenPersisted() {
        return refreshTokenPersisted;
    }

    public void setRefreshTokenPersisted(boolean refreshTokenPersisted) {
        this.refreshTokenPersisted = refreshTokenPersisted;
    }

    public String getRefreshTokenEntity() {
        return refreshTokenEntity;
    }

    public void setRefreshTokenEntity(String refreshTokenEntity) {
        this.refreshTokenEntity = refreshTokenEntity;
    }

    public boolean isRememberMeEnabled() {
        return rememberMeEnabled;
    }

    public void setRememberMeEnabled(boolean rememberMeEnabled) {
        this.rememberMeEnabled = rememberMeEnabled;
    }

    public String getRememberMeMode() {
        return rememberMeMode;
    }

    public void setRememberMeMode(String rememberMeMode) {
        this.rememberMeMode = rememberMeMode;
    }

    public boolean isRegistrationEnabled() {
        return registrationEnabled;
    }

    public void setRegistrationEnabled(boolean registrationEnabled) {
        this.registrationEnabled = registrationEnabled;
    }

    public String getSecurityStyle() {
        return securityStyle;
    }

    public void setSecurityStyle(String securityStyle) {
        this.securityStyle = securityStyle;
    }

    public boolean isStaticUserFallback() {
        return staticUserFallback;
    }

    public void setStaticUserFallback(boolean staticUserFallback) {
        this.staticUserFallback = staticUserFallback;
    }

    public boolean isTestUsersEnabled() {
        return testUsersEnabled;
    }

    public void setTestUsersEnabled(boolean testUsersEnabled) {
        this.testUsersEnabled = testUsersEnabled;
    }

    // ======== Inner Classes ========

    // Inner class for role definitions
    public static class RoleDefinition {
        private String name;
        private String description;
        private List<String> permissions;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public List<String> getPermissions() {
            return permissions;
        }

        public void setPermissions(List<String> permissions) {
            this.permissions = permissions;
        }
    }

    // Inner class for social provider configuration
    public static class SocialProviderConfig {
        private String clientId;
        private String clientSecret;
        private String redirectUri;

        public SocialProviderConfig() {}

        public SocialProviderConfig(String clientId, String clientSecret) {
            this.clientId = clientId;
            this.clientSecret = clientSecret;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getRedirectUri() {
            return redirectUri;
        }

        public void setRedirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
        }
    }
}
