# Quickstart: Payment SAGA Contracts

## Objective

This quickstart defines the local bootstrap sequence for the Payment SAGA Contracts monorepo so a developer can provision infrastructure, validate contracts, run the monorepo build, and inspect metrics and dashboards with Prometheus and Grafana.

## Prerequisites

- Java 21 installed and available on `PATH`
- Maven Wrapper available in repository root (`mvnw.cmd` on Windows)
- Docker Desktop running with Compose support
- Network access for Maven dependency resolution

## Repository Preparation

1. Checkout the feature branch `001-payment-saga-contracts`.
2. Confirm the repository contains the feature artifacts:
   - `specs/001-payment-saga-contracts/spec.md`
   - `specs/001-payment-saga-contracts/plan.md`
   - `specs/001-payment-saga-contracts/data-model.md`
   - `specs/001-payment-saga-contracts/research.md`
   - `specs/001-payment-saga-contracts/tasks.md`
3. Confirm the authoritative contracts exist under `.specs/openapi/` and `.specs/asyncapi/`.

## Step 1: Start Shared Infrastructure

From repository root, start the local platform:

```powershell
docker compose -f infra/docker/docker-compose.yml up -d
```

Expected platform services:

- MySQL 8.0 with logical schema bootstrap
- Redis
- RabbitMQ with Management UI
- Kafka
- Kafka UI
- Prometheus
- Grafana

## Step 2: Verify Infrastructure Health

Run the following checks after containers start:

```powershell
docker compose -f infra/docker/docker-compose.yml ps
docker compose -f infra/docker/docker-compose.yml logs mysql --tail 50
docker compose -f infra/docker/docker-compose.yml logs prometheus --tail 50
docker compose -f infra/docker/docker-compose.yml logs grafana --tail 50
```

Minimum expected outcomes:

- MySQL initialized the logical schemas from `infra/docker/mysql/init/00-create-schemas.sql`
- Prometheus loaded `infra/prometheus/prometheus.yml`
- Grafana loaded datasource and dashboard provisioning from `infra/grafana/provisioning/`

## Step 3: Validate Contract Artifacts Before Code Execution

The OpenAPI and AsyncAPI files are the source of truth and must be validated before service builds are trusted.

Repository paths to inspect before the verification build:

- `.specs/openapi/api-gateway.yaml`
- `.specs/openapi/ms-faturas.yaml`
- `.specs/openapi/ms-pagamentos.yaml`
- `.specs/openapi/ms-comprovantes.yaml`
- `.specs/asyncapi/pagar-event.yaml`
- `.specs/asyncapi/comprovante-gerado.yaml` for both `ComprovanteQueueMessage` and `comprovante.gerado.topic`
- `.specs/asyncapi/comprovante-gerado-dlt.yaml`
- `.specs/asyncapi/notificacoes-consumer.yaml`
- `.specs/asyncapi/problema-fatura-routing.yaml`

Then execute the monorepo verification build:

```powershell
.\mvnw.cmd -q verify
```

Expected validation gates:

- OpenAPI drift checks pass
- AsyncAPI drift checks pass
- PACT tests execute
- Testcontainers-backed integration tests execute
- Jacoco coverage remains at or above 80%

## Step 4: Run the Service Build in Chronological Order

The monorepo is intended to come up in the same order as the plan phases.

Recommended build progression:

1. Parent build and shared libraries
2. `apps/api-gateway`
3. `apps/ms-faturas`
4. `apps/ms-backoffice`
5. `apps/ms-comprovantes`
6. `apps/ms-pagamentos`
7. `apps/ms-notificacoes`

Example Maven commands once the monorepo structure exists:

```powershell
.\mvnw.cmd -pl libs/observability-starter,libs/contract-test-kit,libs/testcontainers-support -am verify
.\mvnw.cmd -pl apps/api-gateway -am verify
.\mvnw.cmd -pl apps/ms-faturas -am verify
.\mvnw.cmd -pl apps/ms-backoffice -am verify
.\mvnw.cmd -pl apps/ms-comprovantes -am verify
.\mvnw.cmd -pl apps/ms-pagamentos -am verify
.\mvnw.cmd -pl apps/ms-notificacoes -am verify
```

For the US2 retry-exhaustion validation, `apps/ms-backoffice` must already be available because the independently testable outcome includes persisted Backoffice intake data and exactly-once routing assertions.

## Step 5: Smoke-Test the Runtime Contracts

### Auth login

Call the public login route:

```powershell
curl.exe -i -X POST http://localhost:8080/api/v1/auth/login `
  -H "Content-Type: application/json" `
  -d '{"username":"admin","password":"admin123"}'
```

Expected outcome:

- Response includes `Authorization: Bearer ...`
- Token TTL is 20 minutes

### Faturas flow

Use the token to create or trigger a fatura payment request after the service is available.

Expected outcome:

- JWT is accepted downstream
- `trace_id` is propagated
- `POST /api/v1/faturas/{id}/pagamentos` dispatches `PAGAR`

### Comprovantes flow

Call `POST /api/v1/comprovantes`.

Expected outcome:

- `202 Accepted`
- UUID v4 returned immediately
- RabbitMQ message published
- MySQL stores `payload_notificacao_completo`

### SAGA finality rule

Inspect payment state transitions.

Expected outcome:

- `PAGO` is never persisted before successful consumption of `comprovante.gerado.topic`

## Step 6: Inspect Observability

### Prometheus

Open:

- `http://localhost:9090`

Verify scrape targets for:

- `api-gateway`
- `ms-faturas`
- `ms-pagamentos`
- `ms-comprovantes`
- `ms-notificacoes`
- `ms-backoffice`

Key metrics to query:

- `auth_login_requests_total`
- `faturas_retry_exhausted_total`
- `pagamentos_pago_confirmed_total`
- `comprovantes_cache_miss_total`
- `notificacoes_dlt_total`
- `trace_propagation_failures_total`

### Grafana

Open:

- `http://localhost:3000`

Expected provisioned dashboards:

- Gateway Auth Overview
- Faturas Lifecycle
- Pagamentos SAGA
- Comprovantes Throughput
- Notificacoes DLT Health
- Trace and Log Correlation

## Step 7: Validate Retry and Failure Paths

Critical checks:

1. Force a mock gateway number `0`, `4`, or `9` and verify `RECUSADO` plus compensation.
2. Trigger repeated `RECUSADO` retries and verify transition to `PROBLEMA` after the third attempt.
3. Force notification processing failure and verify routing to `comprovante.gerado.DLT` after 3 attempts.
4. Force a comprovante cache miss and verify MySQL fallback plus max 3 GET retries before `404`.

## Step 8: Shutdown

To stop the local platform:

```powershell
docker compose -f infra/docker/docker-compose.yml down
```

To remove associated volumes as well:

```powershell
docker compose -f infra/docker/docker-compose.yml down -v
```

## Done Criteria

The local environment is considered ready when all of the following are true:

- Containers are healthy
- Contracts pass verification
- Build passes with tests and Jacoco >= 80%
- `trace_id` appears in logs and transport flows
- Grafana dashboards are provisioned and populated
- The SAGA rule preventing synchronous `PAGO` is observable and enforced