#!/usr/bin/env bash
set -Eeuo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"
cp -n .env.example .env || true
set_env(){
  local key="$1" value="$2"
  if grep -q "^${key}=" .env; then sed -i "s/^${key}=.*/${key}=${value}/" .env; else echo "${key}=${value}" >> .env; fi
}
# Smoke uses explicit dev auth because it verifies local behavior without requiring token minting.
set_env DEV_AUTH_ENABLED true
set_env BROKER_ENABLED false
set_env API_HOST_PORT 18080
set_env KEYCLOAK_PORT 18081
set_env DB_HOST_PORT 13307
set_env REDPANDA_KAFKA_HOST_PORT 19093
set_env REDPANDA_HTTP_HOST_PORT 19644
cleanup(){ docker compose --env-file .env down -v --remove-orphans >/dev/null 2>&1 || true; }
trap cleanup EXIT

API_URL="http://localhost:18080"
docker compose --env-file .env up -d --build mariadb keycloak redpanda api
for i in {1..90}; do
  if curl -fsS "$API_URL/health" >/dev/null; then break; fi
  sleep 2
  if [ "$i" -eq 90 ]; then docker compose --env-file .env logs api; echo "FAIL: API did not become healthy"; exit 1; fi
done
curl -fsS "$API_URL/ready" | grep -q READY
TENANT="tenant-smoke-$(date +%s)"
CONN="conn-smoke"
CORR="corr-smoke-$(date +%s)"
DLQ="corr-dlq-$(date +%s)"
curl -fsS -H 'content-type: application/json' -d "{\"id\":\"$TENANT\",\"name\":\"Smoke Test Institution\",\"status\":\"ACTIVE\"}" "$API_URL/api/tenants" >/tmp/fabric-tenant.json
curl -fsS -H 'content-type: application/json' -d "{\"id\":\"$CONN\",\"name\":\"Smoke LMS\",\"systemType\":\"LMS\",\"baseUrl\":\"https://smoke.example.invalid\",\"credentialReference\":\"vault://$TENANT/lms\",\"status\":\"ACTIVE\"}" "$API_URL/api/tenants/$TENANT/connectors" >/tmp/fabric-connector.json
curl -fsS -H 'content-type: application/json' -d "{\"tenant_id\":\"$TENANT\",\"connector_id\":\"$CONN\",\"correlation_id\":\"$CORR\",\"data\":{\"courseId\":\"SMOKE-101\",\"title\":\"Smoke Testing\"}}" "$API_URL/api/events/lms-course" | grep -q ACCEPTED
curl -fsS "$API_URL/api/tenants/$TENANT/traces/$CORR" | grep -q validate-persist
curl -fsS -H 'content-type: application/json' -d "{\"tenant_id\":\"$TENANT\",\"correlation_id\":\"$DLQ\",\"data\":{\"forceFailure\":true}}" "$API_URL/api/events/lms-course" | grep -q DLQ
curl -fsS "$API_URL/api/tenants/$TENANT/dlq" | grep -q "$DLQ"
echo "PASS enterprise-integration-fabric smoke: tenant=$TENANT correlation=$CORR dlq=$DLQ"
