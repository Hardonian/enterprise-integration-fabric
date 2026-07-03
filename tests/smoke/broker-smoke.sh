#!/usr/bin/env bash
set -Eeuo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"
cp -n .env.example .env || true
set_env(){
  local key="$1" value="$2"
  if grep -q "^${key}=" .env; then sed -i "s/^${key}=.*/${key}=${value}/" .env; else echo "${key}=${value}" >> .env; fi
}
set_env DEV_AUTH_ENABLED true
set_env BROKER_ENABLED true
set_env API_HOST_PORT 18082
set_env KEYCLOAK_PORT 18083
set_env DB_HOST_PORT 13308
set_env REDPANDA_KAFKA_HOST_PORT 19094
set_env REDPANDA_HTTP_HOST_PORT 19645
cleanup(){ docker compose --env-file .env down -v --remove-orphans >/dev/null 2>&1 || true; }
trap cleanup EXIT

API_URL="http://localhost:18082"
docker compose --env-file .env up -d --build mariadb keycloak redpanda api
for i in {1..90}; do
  if curl -fsS "$API_URL/health" >/dev/null; then break; fi
  sleep 2
  if [ "$i" -eq 90 ]; then docker compose --env-file .env logs api redpanda; echo "FAIL: API did not become healthy"; exit 1; fi
done
TENANT="tenant-broker-$(date +%s)"
CONN="conn-broker"
CORR="corr-broker-$(date +%s)"
curl -fsS -H 'content-type: application/json' -d "{\"id\":\"$TENANT\",\"name\":\"Broker Smoke Institution\",\"status\":\"ACTIVE\"}" "$API_URL/api/tenants" >/tmp/fabric-broker-tenant.json
curl -fsS -H 'content-type: application/json' -d "{\"id\":\"$CONN\",\"name\":\"Broker LMS\",\"systemType\":\"LMS\",\"baseUrl\":\"https://broker-smoke.invalid\",\"credentialReference\":\"vault://$TENANT/lms\",\"status\":\"ACTIVE\"}" "$API_URL/api/tenants/$TENANT/connectors" >/tmp/fabric-broker-connector.json
curl -fsS -H 'content-type: application/json' -d "{\"tenant_id\":\"$TENANT\",\"connector_id\":\"$CONN\",\"correlation_id\":\"$CORR\",\"data\":{\"courseId\":\"BROKER-101\",\"title\":\"Broker Verification\"}}" "$API_URL/api/events/lms-course" | grep -q ACCEPTED
for i in {1..30}; do
  if docker compose --env-file .env exec -T redpanda rpk topic consume lms.course.created --brokers redpanda:9092 -n 1 -o start 2>/tmp/fabric-rpk.err | tee /tmp/fabric-broker-message.json | grep -q "$CORR"; then
    echo "PASS enterprise-integration-fabric broker smoke: topic=lms.course.created correlation=$CORR"
    exit 0
  fi
  sleep 1
done
echo "FAIL: broker message not observed for $CORR"
docker compose --env-file .env logs api redpanda | tail -200
cat /tmp/fabric-rpk.err || true
exit 1
