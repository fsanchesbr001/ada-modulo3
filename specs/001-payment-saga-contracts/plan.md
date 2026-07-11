# Implementation Plan: Payment SAGA Contracts

**Branch**: `[001-payment-saga-contracts]` | **Date**: 2026-07-11 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-payment-saga-contracts/spec.md`

**Note**: This plan converts the approved SDD V14 specification into a monorepo execution strategy with contract-first delivery, shared infrastructure first, and bounded-context isolation enforced from the start.

## Summary

Build the distributed PIX payment monorepo in four strictly ordered phases so that contracts and infrastructure exist before downstream service code depends on them.

- **Phase 1 - Foundation of shared infrastructure and contracts**: provision local container infrastructure, isolate OpenAPI and AsyncAPI artifacts under `.specs/`, and refactor the build into a parent monorepo with shared dependency management for Spring Boot 3.x, PACT, Testcontainers, and Jacoco.
- **Phase 2 - Edge security, entry domain, and exhausted-retry escalation**: implement the API Gateway auth boundary, BCrypt-based Flyway seed for `db_auth`, the `ms-faturas` bounded context with MySQL truth, Redis state projection, and the 3-attempt `RECUSADO -> PROBLEMA` worker, plus the minimal `ms-backoffice` routing slice required to capture exhausted retries.
- **Phase 3 - Transactionality, throughput, and active contracts**: implement `ms-comprovantes` high-throughput async ingestion, RabbitMQ consumer persistence with JSON payload retention, cache-aside retrieval, and the `ms-pagamentos` SAGA orchestrator with the hard prohibition on synchronous `PAGO` finalization.
- **Phase 4 - Alerts, operational visibility, and automated delivery**: implement `ms-notificacoes` and GitHub Actions CI/CD with staged contract verification, Testcontainers-backed integration tests, multi-stage Docker image builds, and the remaining observability dashboards.

This feature also requires two first-class design outputs before tasks are generated:

- `data-model.md` defines relational schemas, Redis keyspaces, lifecycle states, and event payload structures for each bounded context.
- `research.md` records observability decisions, including mandatory Micrometer metrics, Prometheus scrape targets, Grafana dashboard design, and trace/log correlation strategy.

The existing root build is currently a single-module Spring Boot application using Spring Boot 4.1.0. This feature plan realigns the repository to a Spring Boot 3.x Java 21 monorepo because that is a mandatory platform constraint from the project constitution and the approved scope.

## Technical Context

**Language/Version**: Java 21, Spring Boot 3.x

**Primary Dependencies**: Spring Cloud Gateway, Spring Security Crypto, JWT HMAC-SHA256 support, Spring Data JPA, Spring Data Redis, Flyway, Spring AMQP, Spring for Apache Kafka, Micrometer/Prometheus, PACT, Testcontainers, Jacoco

**Storage**: MySQL 8.0 with isolated logical schemas (`db_auth`, `db_faturas`, plus service-owned schemas as needed), Redis for cache-aside and high-speed state, RabbitMQ Direct Exchange, Apache Kafka with DLT support

**Testing**: JUnit 5, Spring Boot Test, Testcontainers, PACT for HTTP and messaging, Jacoco coverage gate >= 80%

**Target Platform**: Dockerized Linux services executed locally with Docker Compose and validated in GitHub Actions

**Project Type**: Multi-module backend monorepo with independently deployable Spring Boot services and shared technical libraries

**Performance Goals**: `POST /api/v1/comprovantes` returns `202 Accepted` without blocking on downstream persistence, Redis-backed reads serve the hot path for faturas and comprovantes, and message-driven flows sustain asynchronous processing without violating retry ceilings

**Constraints**: OpenAPI and AsyncAPI are authoritative, all services follow Hexagonal Architecture, JWT TTL is fixed at 20 minutes, `trace_id` propagation is mandatory, retry ceilings are hard-capped at 3, `PAGO` cannot be persisted before `comprovante.gerado.topic` confirmation, and BCrypt is mandatory for seeded and runtime credentials

**Scale/Scope**: 6 bounded contexts (`api-gateway`, `ms-faturas`, `ms-pagamentos`, `ms-comprovantes`, `ms-notificacoes`, `ms-backoffice`) plus shared contract, infrastructure, observability, and CI/CD assets

**Data Model Scope**: logical MySQL schemas for authentication, faturas truth, pagamentos orchestration state, comprovantes retention, and backoffice audit; Redis keyspaces for hot reads and transient workflow state; canonical event payloads for `PAGAR`, `ComprovanteQueueMessage` and `comprovante.gerado.topic` in `.specs/asyncapi/comprovante-gerado.yaml`, plus `comprovante.gerado.DLT`

**Observability Scope**: mandatory trace propagation, structured JSON logs, Prometheus metrics, and Grafana dashboards for gateway auth traffic, faturas lifecycle, payment SAGA health, comprovantes throughput/cache behavior, and notification retry/DLT visibility

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] Bounded contexts are explicitly identified and impacted modules are listed.
- [x] Domain layer remains hexagonal and framework-agnostic (no Spring/JPA/
  messaging dependencies in domain).
- [x] Clean Code/SOLID compliance strategy is documented (naming, constants,
  single-responsibility boundaries).
- [x] OpenAPI is the source of truth for every HTTP controller, endpoint,
  request/response DTO, and generated/validated REST contract.
- [x] AsyncAPI is the source of truth for every producer, consumer, listener,
  topic, queue, and payload schema.
- [x] Testing strategy includes unit + integration tests for all endpoints and
  PACT coverage for HTTP and messaging contracts.
- [x] Coverage plan enforces Jacoco >= 80% in CI/CD.
- [x] Trace strategy defines trace_id generation and propagation for HTTP and
  asynchronous messaging across SAGA flows.
- [x] Observability plan includes structured JSON logs with MDC keys and
  Micrometer/Prometheus metrics (including compensations and DLQ/DLT).
- [x] Data plan includes Flyway migrations, JPA/Redis usage, and JSON payload
  persistence requirement for Receipts/Comprovantes where applicable.
- [x] Messaging resilience defines RetryableTopic and DLQ/DLT behavior for
  external-system Kafka consumers.
- [x] SAGA plan prohibits synchronous PAGO persistence before successful
  consumption of comprovante.gerado.topic and defines the PROBLEMA fallback.
- [x] Retry policy enforces a hard limit of 3 attempts for cache-aside, Kafka,
  and Faturas retry workers.
- [x] Security plan enforces BCrypt for password handling in Auth/API Gateway
  and Flyway seed data.
- [x] Internal security plan enforces JWT validation for service-to-service
  Authorization: Bearer propagation.

## Project Structure

### Documentation (this feature)

```text
specs/001-payment-saga-contracts/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── openapi/
│   │   ├── api-gateway.yaml
│   │   ├── ms-faturas.yaml
│   │   ├── ms-pagamentos.yaml
│   │   └── ms-comprovantes.yaml
│   └── asyncapi/
│       ├── pagar-event.yaml
│       ├── comprovante-gerado.yaml      # owns ComprovanteQueueMessage and comprovante.gerado.topic
│       ├── comprovante-gerado-dlt.yaml
│       ├── notificacoes-consumer.yaml
│       └── problema-fatura-routing.yaml
└── tasks.md
```

### Source Code (repository root)

```text
.specs/
├── openapi/
│   ├── api-gateway.yaml
│   ├── ms-faturas.yaml
│   ├── ms-pagamentos.yaml
│   └── ms-comprovantes.yaml
└── asyncapi/
    ├── pagar-event.yaml
    ├── comprovante-gerado.yaml
  ├── comprovante-gerado-dlt.yaml
  ├── notificacoes-consumer.yaml
  └── problema-fatura-routing.yaml

.github/
└── workflows/
    └── ci.yml

apps/
├── api-gateway/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/fabriciosanches/adamodulo3/apigateway/
│       │   ├── domain/
│       │   ├── application/
│       │   ├── adapter/in/web/
│       │   ├── adapter/out/security/
│       │   ├── adapter/out/token/
│       │   └── config/
│       └── test/
├── ms-faturas/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/fabriciosanches/adamodulo3/faturas/
│       │   ├── domain/
│       │   ├── application/
│       │   ├── adapter/in/web/
│       │   ├── adapter/in/scheduler/
│       │   ├── adapter/out/persistence/mysql/
│       │   ├── adapter/out/persistence/redis/
│       │   ├── adapter/out/messaging/
│       │   └── config/
│       └── test/
├── ms-pagamentos/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/fabriciosanches/adamodulo3/pagamentos/
│       │   ├── domain/
│       │   ├── application/
│       │   ├── adapter/in/web/
│       │   ├── adapter/in/messaging/
│       │   ├── adapter/out/persistence/mysql/
│       │   ├── adapter/out/messaging/
│       │   ├── adapter/out/gatewaymock/
│       │   └── config/
│       └── test/
├── ms-comprovantes/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/fabriciosanches/adamodulo3/comprovantes/
│       │   ├── domain/
│       │   ├── application/
│       │   ├── adapter/in/web/
│       │   ├── adapter/in/messaging/
│       │   ├── adapter/out/persistence/mysql/
│       │   ├── adapter/out/persistence/redis/
│       │   ├── adapter/out/messaging/
│       │   └── config/
│       └── test/
├── ms-notificacoes/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/fabriciosanches/adamodulo3/notificacoes/
│       │   ├── domain/
│       │   ├── application/
│       │   ├── adapter/in/messaging/
│       │   ├── adapter/out/notification/
│       │   └── config/
│       └── test/
└── ms-backoffice/
    ├── pom.xml
    └── src/
        ├── main/java/com/fabriciosanches/adamodulo3/backoffice/
        │   ├── domain/
        │   ├── application/
        │   ├── adapter/in/messaging/
        │   ├── adapter/out/persistence/mysql/
        │   └── config/
        └── test/

infra/
├── docker/
│   ├── docker-compose.yml
│   └── mysql/init/
│       └── 00-create-schemas.sql
├── grafana/
│   ├── provisioning/
│   │   ├── dashboards/
│   │   └── datasources/
│   └── dashboards/
│       ├── gateway-auth-overview.json
│       ├── faturas-lifecycle.json
│       ├── pagamentos-saga.json
│       ├── comprovantes-throughput.json
│       └── notificacoes-dlt.json
└── github-actions/

libs/
├── observability-starter/
├── contract-test-kit/
└── testcontainers-support/

pom.xml
```

**Structure Decision**: Adopt a Maven parent monorepo with `pom.xml` at the root using `packaging=pom`, six application modules under `apps/`, and only technical shared libraries under `libs/`. Domain code stays inside each bounded context and is never shared as a common business library. Contracts are isolated under `.specs/` before Java implementation starts. Infrastructure manifests live under `infra/`, including Grafana provisioning and dashboard JSON. Delivery is chronological: Phase 1 provisions containers, contracts, parent build, and observability baseline; Phase 2 unlocks authentication, `ms-faturas`, and the minimal `ms-backoffice` exhausted-retry intake slice; Phase 3 unlocks `ms-comprovantes` and `ms-pagamentos`; Phase 4 unlocks `ms-notificacoes`, Grafana dashboards, and the full CI/CD pipeline.

## Complexity Tracking

No constitution violations are required for this plan. The monorepo split, shared technical libraries, and phased rollout are implementation choices made to preserve contract-first delivery, hexagonal isolation, and infrastructure-first sequencing.