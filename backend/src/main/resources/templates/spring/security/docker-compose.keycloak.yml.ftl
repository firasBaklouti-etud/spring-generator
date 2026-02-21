<#assign realm = security.keycloakRealm!"my-realm">
version: '3.8'

services:
  keycloak:
    image: quay.io/keycloak/keycloak:latest
    container_name: keycloak
    command:
      - start-dev
      - --import-realm
    environment:
      - KEYCLOAK_ADMIN=admin
      - KEYCLOAK_ADMIN_PASSWORD=admin
      - KC_HTTP_PORT=8180
    ports:
      - "8180:8180"
    volumes:
      - ./keycloak-realm.json:/opt/keycloak/data/import/${realm}-realm.json:ro
    networks:
      - spring-network
    restart: unless-stopped

networks:
  spring-network:
    driver: bridge
