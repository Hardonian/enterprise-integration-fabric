# Production deployment notes

This repository includes two Compose modes:

- `docker-compose.yml`: local developer stack with SvelteKit dev server and imported Keycloak realm.
- `docker-compose.prod.yml`: production-style stack with restart policies, persistent broker and Keycloak volumes, static dashboard served by Nginx, dev auth forced off, and broker publishing forced on.

Production traffic should terminate TLS before the services. A simple customer-tomorrow deployment can use Caddy or Nginx in front of:

- API container on internal port 8080
- dashboard container on internal port 80
- Keycloak on internal port 8080, or a managed OIDC provider

Required production controls:

1. `DEV_AUTH_ENABLED=false`
2. `BROKER_ENABLED=true`
3. strong MariaDB and Keycloak admin passwords
4. `CORS_ALLOWED_ORIGINS` restricted to the dashboard origin
5. JWT tokens include either:
   - `tenant_id`: single tenant
   - `tenant_ids`: array of allowed tenants
   - `realm_access.roles` containing `fabric-admin` for cross-tenant operators
6. connector credentials live in a vault; connector rows store only references
7. external backups for MariaDB and Redpanda volumes
8. log/metric collection attached to API, broker, database, and ingress

The production Compose file is a reference deployment, not a substitute for managed backups, WAF, TLS automation, centralized secrets, or enterprise IdP operations.
