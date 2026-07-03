# Enterprise Integration Fabric

## Executive summary

Enterprise Integration Fabric is a production-grade reference architecture for connecting LMS, SIS, CRM, billing, identity, analytics, and support systems through a governed integration layer. It combines Kotlin, Spring Boot 3, Apache Camel 4, Redpanda/Kafka-compatible messaging, Keycloak OIDC, MariaDB, SvelteKit, OpenAPI, AsyncAPI, Docker Compose, Testcontainers, ADRs, runbooks, and Mermaid diagrams.

The repo is intentionally vendor-neutral. It demonstrates how an enterprise architect would separate customer systems, partner systems, product systems, identity, operational telemetry, and commercial workflows without hardcoding vendor secrets or collapsing tenant boundaries.

## Business problem

Institutions usually integrate LMS, SIS, CRM, billing, support, analytics, and identity through point-to-point APIs. That creates fragile credentials, inconsistent error handling, weak replayability, unclear ownership, and expensive incident response. This fabric centralizes event contracts, tenant scoping, traceability, dead-letter handling, and identity boundaries.

## Stakeholder map

| Stakeholder | Concern | Fabric response |
| --- | --- | --- |
| CIO / CTO | Reduce integration risk | Contract-first event/API boundary with explicit extension points |
| Security / IAM | OIDC, secrets, least privilege | Keycloak resource-server validation; credential references only |
| Customer success / support | Trace incidents quickly | Correlation-id trace lookup and DLQ runbook |
| Product / platform | Reliable LMS/SIS/CRM/billing workflows | Camel routes with retry, trace, and DLQ fallback |
| Data / analytics | Consistent events | CloudEvents-like AsyncAPI contracts |
| Implementation partners | Repeatable onboarding | Tenant, connector, and runbook-driven onboarding |

## Current-state architecture

See docs/architecture/current-state.md. The current-state pattern is direct point-to-point integrations with duplicated credentials and weak operational visibility.

## Target-state architecture

See docs/architecture/target-state.md and docs/diagrams/container.mmd. Target state routes all integration traffic through tenant-scoped APIs and Camel event routes, records operational state in MariaDB, publishes events to Redpanda, validates identity through Keycloak, and exposes an operational SvelteKit dashboard.

## Local setup

Requirements:

- Docker and Docker Compose
- Node 24 + pnpm for direct frontend commands
- Make
- Internet access the first time Gradle/Node/Docker dependencies are pulled

Commands:

```bash
make setup
make build
make test
make smoke
make smoke-broker
```

For interactive local stack:

```bash
cp -n .env.example .env
# For local no-token development only:
sed -i 's/^DEV_AUTH_ENABLED=.*/DEV_AUTH_ENABLED=true/' .env
make dev
```

Endpoints:

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- Dashboard: http://localhost:5173
- Keycloak: http://localhost:8081
- Redpanda Kafka: localhost:19092
- MariaDB: localhost:3307

Production-style render check:

```bash
cp .env.production.example .env.production
# replace every replace_with_* value before real use
make prod-config
```

Go-live runbook: docs/runbooks/customer-go-live.md.

## Demo flow

1. Create a tenant with POST /api/tenants.
2. Register an LMS connector with POST /api/tenants/{tenantId}/connectors.
3. Ingest examples/payloads/lms-course-created.json at POST /api/events/lms-course.
4. Search by correlation ID at GET /api/tenants/{tenantId}/traces/{correlationId}.
5. Ingest examples/payloads/force-dlq.json to verify retry exhaustion and DLQ persistence.
6. View DLQ at GET /api/tenants/{tenantId}/dlq.

The smoke script executes this flow end to end.

## Integration patterns

- API gateway pattern: Spring controllers expose tenant-scoped management and ingestion endpoints.
- Router pattern: Apache Camel routes normalize envelopes and dispatch events.
- Publish-subscribe pattern: Redpanda/Kafka-compatible topics carry integration events when BROKER_ENABLED=true.
- Dead-letter pattern: failed route processing writes tenant-scoped DLQ rows after retry exhaustion.
- Operational trace pattern: every route writes event_traces by tenant_id and correlation_id.
- Credential reference pattern: connectors store vault-style references, never raw secrets.

## Tenant isolation model

Application-layer isolation:

- Connector, trace, DLQ, entitlement, and event routes are tenant-scoped.
- Cross-tenant connector lookup returns not found.
- Event ingestion requires tenant_id and rejects unknown tenants.

Database-layer isolation:

- Every tenant-owned table includes tenant_id.
- tenant_id indexes exist for tenant-owned lookup paths.
- Foreign keys bind tenant-owned records to tenants.

Details: docs/architecture/tenant-isolation.md.

## Identity model

Keycloak runs locally through Docker Compose. The backend validates JWTs as an OAuth2 resource server by default. Local developer bypass is explicit and disabled by default:

```env
DEV_AUTH_ENABLED=false
```

Set DEV_AUTH_ENABLED=true only for local smoke/dev workflows where token minting is not the subject under test.

## Event contract model

AsyncAPI contract: contracts/asyncapi/integration-fabric.asyncapi.yaml.

Each event uses a CloudEvents-like envelope:

- id
- source
- specversion
- type
- subject
- time
- datacontenttype
- tenant_id
- correlation_id
- data

## Failure handling

Camel routes validate payload shape, enforce tenant_id, add correlation_id if absent, persist accepted events, publish to the broker when enabled, and write traces. Route failures retry twice and then persist a dead_letter_events row with the failure reason and original payload.

Runbook: docs/runbooks/replay-failed-event.md.

## Security model

- OIDC validation enabled by default.
- Dev auth is opt-in only.
- .env.example contains non-production defaults only.
- Raw connector secrets are not stored; only credential references are persisted.
- API errors are structured and do not expose stack traces.

Details: docs/architecture/security-model.md.

## Observability model

This reference uses deterministic operational tables for local demonstration:

- integration_events for accepted event state
- event_traces for route-level steps
- dead_letter_events for failure triage
- health/readiness/version endpoints for service checks

Production extension points include OpenTelemetry, Prometheus metrics, centralized logs, and broker lag dashboards.

## Trade-offs

- MariaDB is used for operational state; analytical projections should be built downstream.
- Local Compose uses Redpanda for Kafka compatibility; managed Kafka can replace it without changing event contracts.
- Dev auth accelerates local smoke tests but remains disabled by default.
- The dashboard is intentionally operational, not a full admin product.

## Extension points

Working stubs exist for grade sync, CRM handoff, and billing event simulation routes. Real downstream adapters should replace the broker/no-op boundary with vendor-specific Camel components or service clients while preserving envelope, trace, tenant, retry, and DLQ behavior.

## Verification results

Validated in this build session:

| Command | Result |
| --- | --- |
| `make setup` | PASS - copied `.env` if absent and installed SvelteKit dependencies with pnpm |
| `make compose-config` | PASS - Docker Compose rendered successfully to `/tmp/enterprise-integration-fabric-compose.yml` |
| `make prod-config` | PASS - production-style Compose rendered successfully to `/tmp/enterprise-integration-fabric-prod-compose.yml` |
| `make backend-test` / `make test` | PASS - Kotlin/Spring integration tests passed, including dev/JWT-style tenant authorization enforcement; optional Testcontainers probe is gated by `RUN_TESTCONTAINERS=true` because this workstation's Docker daemon rejects docker-java's default API negotiation in nested Gradle containers |
| `make lint` | PASS - backend check green; `svelte-check` returned 0 errors and 0 warnings |
| `make build` | PASS - SvelteKit static build and Spring Boot bootJar built successfully |
| `make smoke` | PASS - Compose stack started on smoke-safe host ports, tenant + connector were created, LMS event trace was found, forced failure created a DLQ row |
| `make smoke-broker` | PASS - Compose stack started with `BROKER_ENABLED=true`; Redpanda topic `lms.course.created` contained the produced correlation ID |
| `npx --yes @redocly/cli lint contracts/openapi/integration-fabric.openapi.yaml` | PASS - OpenAPI valid with no warnings |
| `npx --yes @asyncapi/cli validate contracts/asyncapi/integration-fabric.asyncapi.yaml` | PASS - AsyncAPI valid with no governance issues |
