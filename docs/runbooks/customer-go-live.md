# Customer go-live checklist

Use this checklist before exposing the fabric to customer traffic.

## Required decisions

1. Public hostnames:
   - API hostname, for example `api.fabric.example.com`
   - Dashboard hostname, for example `fabric.example.com`
   - Identity hostname, for example `identity.example.com`
2. TLS termination point: Caddy, Nginx, ALB, Cloudflare Tunnel, or managed ingress.
3. Secret source: platform secret manager or vault. Do not use repository files for production secrets.
4. Customer tenant claim model: JWT `tenant_ids` list, `tenant_id` string, or `fabric-admin` realm role.

## Environment hardening

1. Copy production template:
   ```bash
   cp .env.production.example .env.production
   chmod 600 .env.production
   ```
2. Replace every `replace_with_*` value.
3. Keep `DEV_AUTH_ENABLED=false`.
4. Set `BROKER_ENABLED=true`.
5. Set `OIDC_ISSUER_URI` to the public Keycloak/IdP realm URL.
6. Set `CORS_ALLOWED_ORIGINS` to the dashboard public origin only.
7. Verify production compose renders:
   ```bash
   make prod-config
   ```

## Pre-flight validation

```bash
make test
make lint
make build
make compose-config
make smoke
make smoke-broker
npx --yes @redocly/cli lint contracts/openapi/integration-fabric.openapi.yaml
npx --yes @asyncapi/cli validate contracts/asyncapi/integration-fabric.asyncapi.yaml
```

## Production launch sequence

1. Start database and broker.
2. Start Keycloak or configure managed OIDC provider.
3. Configure realm/client/roles and token claims.
4. Start API with production environment.
5. Start dashboard behind TLS.
6. Create first customer tenant.
7. Register customer connectors using credential references only.
8. Send LMS course event.
9. Verify event trace and topic publication.
10. Trigger one controlled DLQ event and verify runbook ownership.

## Customer acceptance smoke

```bash
curl -fsS https://api.fabric.example.com/health
curl -fsS https://api.fabric.example.com/ready
curl -fsS -H "Authorization: Bearer $TOKEN" https://api.fabric.example.com/api/tenants
```

## No-go conditions

- `DEV_AUTH_ENABLED=true` in production.
- CORS allows `*` or untrusted origins.
- Connector stores raw API secret instead of a reference.
- Customer token can access another tenant.
- DLQ grows without alert/runbook owner.
- OpenAPI or AsyncAPI validation fails.
