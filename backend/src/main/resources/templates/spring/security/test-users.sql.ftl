<#-- Derive table name from principalEntity (e.g. "User" -> "users", "AppUser" -> "app_users") -->
<#function toSnake text>
  <#assign result = "">
  <#list 0..<text?length as i>
    <#assign c = text[i]>
    <#if c?matches("[A-Z]") && (i > 0)>
      <#assign result = result + "_" + c?lower_case>
    <#else>
      <#assign result = result + c?lower_case>
    </#if>
  </#list>
  <#return result>
</#function>
<#assign userTable = toSnake(security.principalEntity) + "s">
<#assign usernameCol = toSnake(security.usernameField)>
<#assign passwordCol = toSnake(security.passwordField)>
-- =============================================================
-- Test user seed data
-- Passwords are BCrypt-encoded:
--   admin123 -> $2a$10$...   |   user123 -> $2a$10$...
-- =============================================================

-- Insert test admin user
INSERT INTO ${userTable} (${usernameCol}, ${passwordCol}<#if security.usernameField != "email">, email</#if>)
VALUES ('admin@admin.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'<#if security.usernameField != "email">, 'admin@admin.com'</#if>)
ON CONFLICT (${usernameCol}) DO NOTHING;

-- Insert test regular user
INSERT INTO ${userTable} (${usernameCol}, ${passwordCol}<#if security.usernameField != "email">, email</#if>)
VALUES ('user@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'<#if security.usernameField != "email">, 'user@test.com'</#if>)
ON CONFLICT (${usernameCol}) DO NOTHING;

<#if security.rbacMode?? && security.rbacMode == "DYNAMIC">
-- =============================================================
-- Roles
-- =============================================================
INSERT INTO roles (name, description) VALUES ('ADMIN', 'Administrator with full access') ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name, description) VALUES ('USER', 'Standard user with basic access') ON CONFLICT (name) DO NOTHING;
<#if security.definedRoles??>
<#list security.definedRoles as role>
<#if role.name != "ADMIN" && role.name != "USER">
INSERT INTO roles (name, description) VALUES ('${role.name}', '${role.description!""}') ON CONFLICT (name) DO NOTHING;
</#if>
</#list>
</#if>

-- =============================================================
-- User–role associations
-- =============================================================
INSERT INTO ${userTable}_roles (${toSnake(security.principalEntity)}_id, roles_id)
SELECT u.id, r.id FROM ${userTable} u, roles r
WHERE u.${usernameCol} = 'admin@admin.com' AND r.name = 'ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM ${userTable}_roles ur WHERE ur.${toSnake(security.principalEntity)}_id = u.id AND ur.roles_id = r.id
  );

INSERT INTO ${userTable}_roles (${toSnake(security.principalEntity)}_id, roles_id)
SELECT u.id, r.id FROM ${userTable} u, roles r
WHERE u.${usernameCol} = 'user@test.com' AND r.name = 'USER'
  AND NOT EXISTS (
    SELECT 1 FROM ${userTable}_roles ur WHERE ur.${toSnake(security.principalEntity)}_id = u.id AND ur.roles_id = r.id
  );

-- =============================================================
-- Role permissions
-- =============================================================
<#if security.definedRoles??>
<#list security.definedRoles as role>
<#if role.permissions??>
<#list role.permissions as perm>
INSERT INTO role_permissions (role_id, permission)
SELECT r.id, '${perm}' FROM roles r
WHERE r.name = '${role.name}'
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission = '${perm}'
  );
</#list>
</#if>
</#list>
</#if>
</#if>
