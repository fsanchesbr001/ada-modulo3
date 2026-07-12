# Research: Payment SAGA Contracts

## Overview

This document records the design decisions that should be treated as settled inputs for task generation and implementation planning.

## Decision 1: Monorepo build baseline

- Use a Maven parent project at the repository root with `packaging=pom`.
- Keep each bounded context in its own Spring Boot application module under `apps/`.
- Keep only technical shared libraries in `libs/`.

Reasoning:
- This preserves bounded-context autonomy while centralizing dependency management for Java 21, Spring Boot 3.x, PACT, and Jacoco.

## Decision 2: Contract-first artifact layout

- Store OpenAPI and AsyncAPI artifacts under `.specs/` at repository root.
- Mirror contract references under `specs/001-payment-saga-contracts/contracts/` for feature-scoped documentation.

Reasoning:
- The constitution requires contract artifacts to be the source of truth before Java implementation begins.

## Decision 3: Persistence split by bounded context

- MySQL 8.0 stores durable truth in logical schemas.
- Redis stores cache-aside read models and high-speed status projections.
- RabbitMQ handles comprovante processing decoupling.
- Kafka handles payment confirmation and notification fan-out.

Reasoning:
- This matches the SDD V14 constraints and isolates infrastructure concerns to context-owned adapters.

## Decision 4: Observability baseline

- Every HTTP ingress and scheduled job creates or continues a `trace_id`.
- Every outbound HTTP call and RabbitMQ/Kafka message propagates `trace_id` in transport headers.
- MDC is mandatory for `trace_id`, `fatura_id`, `pagamento_id`, and `comprovante_id` when known.
- Logs are emitted as structured JSON.

Reasoning:
- Distributed diagnosis and auditability are mandatory for the payment flow.

## Decision 5: Metrics catalog

### Gateway and auth metrics

- `auth_login_requests_total`
- `auth_login_failures_total`
- `jwt_tokens_issued_total`
- `gateway_downstream_propagations_total`
- `gateway_downstream_propagation_failures_total`

### Faturas metrics

- `faturas_lote_created_total`
- `faturas_status_transitions_total{from,to}`
- `faturas_retry_attempts_total`
- `faturas_retry_exhausted_total`
- `faturas_cache_miss_total`

### Pagamentos metrics

- `pagamentos_pagar_events_consumed_total`
- `pagamentos_gateway_mock_recusado_total`
- `pagamentos_gateway_mock_processando_total`
- `pagamentos_compensations_total`
- `pagamentos_pago_confirmed_total`
- `pagamentos_pago_blocked_until_receipt_total`

### Comprovantes metrics

- `comprovantes_post_accepted_total`
- `comprovantes_queue_published_total`
- `comprovantes_consumer_processed_total`
- `comprovantes_consumer_errors_total`
- `comprovantes_cache_hit_total`
- `comprovantes_cache_miss_total`
- `comprovantes_get_retry_attempts_total`

### Notificacoes metrics

- `notificacoes_kafka_consumed_total`
- `notificacoes_retry_attempts_total`
- `notificacoes_dlt_total`

### Platform metrics

- `trace_propagation_failures_total`
- `rabbitmq_publish_failures_total`
- `kafka_publish_failures_total`
- `backoffice_problem_routes_total`

Reasoning:
- Metrics must expose both business flow health and failure-mode visibility.

## Decision 6: Grafana dashboard plan

Provision Grafana locally and in shared environments with Prometheus as the datasource and JSON dashboards versioned under `infra/grafana/dashboards/`.

### Dashboard 1: Gateway Auth Overview

Panels:
- Login request rate
- Login failure rate
- JWT issuance success count
- Downstream token propagation errors
- p95 auth latency

### Dashboard 2: Faturas Lifecycle

Panels:
- Faturas created by batch
- Current status distribution (`PENDENTE`, `SOLICITADO`, `RECUSADO`, `PROBLEMA`)
- Retry attempts over time
- Retry exhaustion count
- Redis hit/miss ratio for `GET /faturas/{id}`

### Dashboard 3: Pagamentos SAGA

Panels:
- `PAGAR` events consumed per minute
- Mock gateway `RECUSADO` vs `PROCESSANDO`
- Compensation count
- `PAGO` confirmations from `comprovante.gerado.topic`
- Violations blocked before synchronous `PAGO`

### Dashboard 4: Comprovantes Throughput

Panels:
- `POST /comprovantes` accepted requests
- RabbitMQ publish/consume rate
- Consumer processing failures
- Cache hit/miss ratio
- GET retry attempts before success or `404`

### Dashboard 5: Notificacoes DLT Health

Panels:
- Kafka messages consumed
- Retry attempts by listener
- DLT volume over time
- Oldest DLT message age

### Dashboard 6: Trace and Log Correlation

Panels:
- Requests/messages missing `trace_id`
- Volume of logs by service and severity
- Correlated flow drill-down by `trace_id`

Reasoning:
- Operators need service-specific dashboards plus one cross-flow trace health dashboard.

## Decision 7: Prometheus scrape and provisioning

- Every Spring Boot service exposes `/actuator/prometheus`.
- Docker Compose provisions Prometheus and Grafana alongside Kafka UI and RabbitMQ Management.
- Grafana dashboards and datasource provisioning are committed to source control.

Reasoning:
- Observability must be reproducible locally and in CI-like environments.

## Decision 8: CI/CD observability gates

- Contract checks must fail on OpenAPI/AsyncAPI drift.
- Test and build jobs must export Jacoco coverage.
- Dashboard JSON and Prometheus scrape configs are validated as versioned assets.

Reasoning:
- Observability cannot remain a manual post-implementation concern.