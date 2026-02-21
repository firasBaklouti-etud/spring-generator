# Plan: Match Bootify Features + Overhaul Security Generation

## Scope
Two main objectives:
1. **General feature parity** with Bootify across the project
2. **Complete security generation overhaul** (both backend code quality and frontend UI/UX)

Given the scale, this plan is organized into priority phases. Each phase builds on the previous one.

---

## Phase 1: Security Generation Overhaul (Priority - HIGH)

This is the user's primary pain point. Both the generated code and the UI need improvement.

### 1A. Backend Security Model Enhancements [x]

**Files to modify:**
- `backend/src/main/java/com/firas/generator/model/config/SecurityConfig.java` [x]
- `backend/src/main/java/com/firas/generator/model/config/SecurityRule.java` [x]
- `backend/src/main/java/com/firas/generator/model/ProjectRequest.java` [x]

**Changes:**
- [x] Add fields to `SecurityConfig`:
  - [x] `signingAlgorithm` (HS256 / RS256) for JWT
  - [x] `socialLogins` (list: GOOGLE, GITHUB, FACEBOOK)
  - [x] `formLoginEnabled` (boolean for SSR form login)
  - [x] `keycloakEnabled`, `keycloakRealm`, `keycloakClientId`, `keycloakClientSecret`, `keycloakIssuerUrl`
  - [x] `passwordResetEnabled`, `passwordResetTokenField`, `passwordResetExpiryField`
  - [x] `refreshTokenPersisted` (boolean - store in DB vs in-memory)
  - [x] `refreshTokenEntity` (table name for refresh tokens)
  - [x] `rememberMeEnabled`, `rememberMeMode` (ALWAYS / CHECKBOX)
  - [x] `registrationEnabled` (boolean toggle)
  - [x] `securityStyle` (ANNOTATION / CONFIG) - @PreAuthorize vs SecurityFilterChain
  - [x] `staticUserFallback` (boolean - if no user table, create in-memory users)
  - [x] `testUsersEnabled` (boolean)

### 1B. New Security Templates (Backend) [/]

**New FreeMarker templates to create in `backend/src/main/resources/templates/spring/security/`:**

1. [x] **RS256 JWT Support:**
   - [x] `JwtUtil_RS256.ftl` - RSA key-based JWT signing/verification (Integrated into `JwtUtil.ftl`)
   - [x] Update existing `JwtUtil.ftl` to conditionally use HS256 or RS256

2. [x] **Social Login:**
   - [ ] `OAuth2LoginConfig.ftl` - Spring Security OAuth2 login config for Google/GitHub/Facebook (Partially handled)
   - [x] `OAuth2UserService.ftl` - Custom OAuth2UserService to map social profiles to local users
   - [x] `SocialAuthController.ftl` - Endpoint to handle social login code exchange for JWT

3. [ ] **Form-Based Login:**
   - [ ] `FormLoginSecurityConfig.ftl` - Security config for form-based SSR authentication
   - [ ] `AuthenticationController.ftl` - MVC controller for login/logout pages (Thymeleaf)
   - [ ] `login.html.ftl` - Thymeleaf login template
   - [ ] `RegistrationController.ftl` - MVC controller for registration page

4. [x] **Keycloak Integration:**
   - [x] `KeycloakResourceServerConfig.ftl` - Security config as resource server (Integrated)
   - [ ] `KeycloakOAuthConfig.ftl` - Security config for OAuth/OIDC client
   - [ ] `keycloak-realm.json.ftl` - Realm export template
   - [ ] `docker-compose.keycloak.yml.ftl` - Docker compose snippet
   - [ ] `UserSynchronizationService.ftl` - Sync Keycloak users to local DB

5. [x] **Password Reset:**
   - [x] `PasswordResetService.ftl` - Token generation, validation, expiry logic
   - [x] `PasswordResetController.ftl` - REST endpoints for request/reset password
   - [x] `MailService.ftl` - Email sending service (using Spring Mail)

6. [x] **Refresh Token Persistence:**
   - [x] `RefreshTokenEntity.ftl` - JPA entity for refresh tokens
   - [x] `RefreshTokenRepository.ftl` - Repository
   - [x] `RefreshTokenService.ftl` - Service for token CRUD, rotation, expiry cleanup
   - [x] Update `AuthController.ftl` to use DB-stored refresh tokens

7. [x] **Remember-Me:**
   - [x] Update `SecurityConfig.ftl` to include `rememberMe(...)` configuration
   - [x] Generate cookie settings and token repository if needed

8. [ ] **Integration Test Helpers:**
   - [ ] `BaseIT.ftl` - Base integration test class with authenticated session/bearer token helpers
   - [ ] `SecurityTestConfig.ftl` - Test security configuration
   - [ ] Test user SQL seed scripts

9. [x] **Annotation-Based Security Option:**
   - [x] Update `Controller.ftl` to conditionally add `@PreAuthorize` annotations

10. [x] **Swagger JWT Integration:**
    - [x] Update `OpenApiConfig.ftl` to add `@SecurityScheme` for Bearer JWT

**Files to modify:**
- `backend/src/main/java/com/firas/generator/stack/spring/SpringStackProvider.java` [x]
- `backend/src/main/java/com/firas/generator/stack/spring/SpringCodeGenerator.java` [x]
- `backend/src/main/resources/templates/spring/SecurityConfig.ftl` [x]
- `backend/src/main/resources/templates/spring/pom.xml.ftl` [x]
- `backend/src/main/resources/templates/spring/application.properties.ftl` [x]

### 1C. Frontend Security UI Overhaul [x]

**File to modify:** `frontend/components/generator/security-phase.tsx` [x]

**New layout (inspired by Bootify's Security tab):**
- [x] 1. **Authentication Type Section** (Cards: JWT, Basic Auth, Form Login, Keycloak)
- [x] 2. **Social Login Panel** (Google, GitHub, Facebook toggles + fields)
- [x] 3. **Keycloak Configuration Panel** (Realm, Client ID, Secret, Issuer)
- [x] 4. **Token & Session Policy Panel** (JWT TTL, Refresh TTL, Persistence, Remember-me)
- [x] 5. **Password Reset Section** (Enable toggle, Token/Expiry field selectors)
- [x] 6. **Security Style Toggle** (Annotation vs Config)
- [x] 7. **Test Users Section**
- [ ] 8. **Live Preview Panel**

**Store updates in `frontend/lib/store.ts`:**
- [x] Add all new SecurityConfig fields to the store interface

---

## Phase 2: General Feature Additions (Priority - MEDIUM)

### 2A. Gradle Support [ ]
### 2B. Kotlin Support [ ]
### 2C. application.yml Support [ ]
### 2D. Lombok Support [x]
### 2E. MongoDB Support [ ]
### 2F. MapStruct Integration [ ]
### 2G. Testcontainers Support [ ]
### 2H. Rest-Assured Support [ ]

---

## Phase 3: Advanced Features (Priority - LOWER)

### 3A. Multi-Module Project Support [ ]
### 3B. Frontend Scaffolding (Thymeleaf/Angular/React) [ ]
### 3C. Custom Template Support [ ]

---

## Implementation Order (Recommended)

1. [x] **Security model expansion**
2. [x] **RS256 JWT + Swagger JWT integration**
3. [x] **Password reset generation**
4. [x] **Refresh Token DB persistence**
5. [x] **Social login support**
6. [x] **Registration toggle + static user fallback** (Implemented in model and templates)
7. [x] **Annotation-based vs config-based security**
8. [x] **Frontend Security UI overhaul**
9. [ ] **Keycloak integration** (Only basic RS support exists)
10. [ ] **Form-based login**
11. [x] **Remember-me**
12. [ ] **Integration test helpers**
13. [ ] **Gradle support**
14. [ ] **application.yml format**
15. [x] **Lombok support**
16. [ ] **Kotlin support**
17. [ ] **MapStruct**
18. [ ] **MongoDB support**
19. [ ] **Testcontainers**
20. [ ] **Rest-Assured**
