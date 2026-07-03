SHELL := /usr/bin/env bash
API_GRADLE := docker run --rm --user root -e DOCKER_API_VERSION=1.44 -v /var/run/docker.sock:/var/run/docker.sock -v $(PWD):/workspace -w /workspace gradle:8.14.2-jdk21 gradle --no-daemon

.PHONY: setup dev test lint build smoke smoke-broker clean compose-config backend-test frontend-build

setup:
	cp -n .env.example .env || true
	cd apps/web && pnpm install --frozen-lockfile=false

dev:
	docker compose --env-file .env up --build

test: backend-test

backend-test:
	$(API_GRADLE) :apps:api:test

lint:
	$(API_GRADLE) :apps:api:check
	cd apps/web && pnpm install --frozen-lockfile=false && pnpm lint

build: frontend-build
	$(API_GRADLE) :apps:api:bootJar

frontend-build:
	cd apps/web && pnpm install --frozen-lockfile=false && pnpm build

compose-config:
	docker compose --env-file .env.example config >/tmp/enterprise-integration-fabric-compose.yml

prod-config:
	cp -n .env.production.example .env.production || true
	docker compose --env-file .env.production -f docker-compose.prod.yml config >/tmp/enterprise-integration-fabric-prod-compose.yml

smoke:
	bash tests/smoke/smoke.sh

smoke-broker:
	bash tests/smoke/broker-smoke.sh

clean:
	docker compose --env-file .env down -v --remove-orphans || true
	rm -rf apps/web/node_modules apps/web/.svelte-kit apps/web/build apps/api/build .gradle
