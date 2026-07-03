# Security model

Keycloak is the local OIDC provider. The backend is a Spring Security OAuth2 resource server unless DEV_AUTH_ENABLED=true. Dev auth is disabled by default. Connectors persist credential references such as vault://tenant/system/key; raw API secrets are never persisted. Structured errors prevent stack trace disclosure.
