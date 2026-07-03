# Deployment model

Local deployment uses Docker Compose for MariaDB, Redpanda, Keycloak, API, and SvelteKit. Production should externalize secrets to a vault, use managed or hardened Kafka-compatible brokers, deploy the API behind an enterprise ingress, and enforce TLS and OIDC at every boundary.
