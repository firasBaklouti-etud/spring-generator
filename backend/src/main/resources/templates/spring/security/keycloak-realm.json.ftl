<#assign realm = security.keycloakRealm!"my-realm">
<#assign clientId = security.keycloakClientId!"my-client">
{
  "realm": "${realm}",
  "enabled": true,
  "sslRequired": "external",
  "registrationAllowed": false,
  "loginWithEmailAllowed": true,
  "duplicateEmailsAllowed": false,
  "resetPasswordAllowed": true,
  "editUsernameAllowed": false,
  "bruteForceProtected": true,
  "roles": {
    "realm": [
      {
        "name": "USER",
        "description": "Default user role"
      },
      {
        "name": "ADMIN",
        "description": "Administrator role"
      }
    ]
  },
  "defaultRoles": ["USER"],
  "clients": [
    {
      "clientId": "${clientId}",
      "enabled": true,
      "publicClient": true,
      "directAccessGrantsEnabled": true,
      "standardFlowEnabled": true,
      "implicitFlowEnabled": false,
      "redirectUris": [
        "http://localhost:8080/*"
      ],
      "webOrigins": [
        "http://localhost:8080"
      ],
      "protocol": "openid-connect",
      "attributes": {
        "post.logout.redirect.uris": "http://localhost:8080/*"
      },
      "defaultClientScopes": [
        "web-origins",
        "profile",
        "roles",
        "email"
      ]
    }
  ]<#if security.testUsersEnabled?? && security.testUsersEnabled>,
  "users": [
    {
      "username": "testuser",
      "enabled": true,
      "emailVerified": true,
      "firstName": "Test",
      "lastName": "User",
      "email": "testuser@example.com",
      "credentials": [
        {
          "type": "password",
          "value": "testuser",
          "temporary": false
        }
      ],
      "realmRoles": ["USER"]
    },
    {
      "username": "testadmin",
      "enabled": true,
      "emailVerified": true,
      "firstName": "Test",
      "lastName": "Admin",
      "email": "testadmin@example.com",
      "credentials": [
        {
          "type": "password",
          "value": "testadmin",
          "temporary": false
        }
      ],
      "realmRoles": ["USER", "ADMIN"]
    }
  ]</#if>
}
