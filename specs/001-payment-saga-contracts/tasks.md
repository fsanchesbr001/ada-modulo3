# Tasks: Payment SAGA Contracts

**Input**: Design documents from `/specs/001-payment-saga-contracts/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `.specify/memory/constitution.md`

**Tests**: Unit tests, integration tests, OpenAPI/AsyncAPI contract validation, HTTP PACT, messaging PACT, and Jacoco coverage gates are mandatory per constitution.

**Organization**: Tasks are grouped by user story inside the plan's strict 4-phase delivery sequence so each story remains independently testable once its own phase tasks are complete while the repository still respects the chronological platform constraints.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel when the referenced files do not overlap and prior blocking tasks are complete
- **[Story]**: User story label for traceability (`[US1]` to `[US5]`)
- Every task includes exact repository paths and implementation intent

## Phase 1: Shared Infra + Contracts + Parent Build

**Purpose**: Establish the contract-first monorepo, shared infrastructure, and reusable quality tooling before any bounded-context code depends on them.

- [x] T001 Refactor the repository root into a Maven parent aggregator with modules for `apps/api-gateway`, `apps/ms-faturas`, `apps/ms-pagamentos`, `apps/ms-comprovantes`, `apps/ms-notificacoes`, `apps/ms-backoffice`, `libs/observability-starter`, `libs/contract-test-kit`, and `libs/testcontainers-support` in `pom.xml`
- [x] T002 [P] Create module build descriptors with Spring Boot 3.x, Java 21, Flyway, Micrometer, PACT, Testcontainers, and Jacoco baselines in `apps/api-gateway/pom.xml`, `apps/ms-faturas/pom.xml`, `apps/ms-pagamentos/pom.xml`, `apps/ms-comprovantes/pom.xml`, `apps/ms-notificacoes/pom.xml`, `apps/ms-backoffice/pom.xml`, `libs/observability-starter/pom.xml`, `libs/contract-test-kit/pom.xml`, and `libs/testcontainers-support/pom.xml`
- [x] T003 [P] Author the authoritative HTTP contracts for gateway, faturas, pagamentos, and comprovantes in `.specs/openapi/api-gateway.yaml`, `.specs/openapi/ms-faturas.yaml`, `.specs/openapi/ms-pagamentos.yaml`, and `.specs/openapi/ms-comprovantes.yaml`
- [x] T004 [P] Author the authoritative messaging contracts for `PAGAR`, RabbitMQ `ComprovanteQueueMessage` receipt work items and Kafka `comprovante.gerado.topic` confirmation in `.specs/asyncapi/comprovante-gerado.yaml`, `comprovante.gerado.DLT` in `.specs/asyncapi/comprovante-gerado-dlt.yaml`, notification consumption in `.specs/asyncapi/notificacoes-consumer.yaml`, and Backoffice problem routing in `.specs/asyncapi/problema-fatura-routing.yaml`, plus `PAGAR` in `.specs/asyncapi/pagar-event.yaml`
- [x] T005 Mirror the approved repository contracts for feature review in `specs/001-payment-saga-contracts/contracts/openapi/api-gateway.yaml`, `specs/001-payment-saga-contracts/contracts/openapi/ms-faturas.yaml`, `specs/001-payment-saga-contracts/contracts/openapi/ms-pagamentos.yaml`, `specs/001-payment-saga-contracts/contracts/openapi/ms-comprovantes.yaml`, `specs/001-payment-saga-contracts/contracts/asyncapi/pagar-event.yaml`, `specs/001-payment-saga-contracts/contracts/asyncapi/comprovante-gerado.yaml` for both `ComprovanteQueueMessage` and `comprovante.gerado.topic`, `specs/001-payment-saga-contracts/contracts/asyncapi/comprovante-gerado-dlt.yaml`, `specs/001-payment-saga-contracts/contracts/asyncapi/notificacoes-consumer.yaml`, and `specs/001-payment-saga-contracts/contracts/asyncapi/problema-fatura-routing.yaml`
- [x] T006 [P] Provision MySQL, Redis, RabbitMQ, Kafka, Prometheus, Grafana, Kafka UI, and RabbitMQ Management for local execution in `infra/docker/docker-compose.yml`
- [x] T007 [P] Bootstrap the logical schemas `db_auth`, `db_faturas`, `db_pagamentos`, `db_comprovantes`, and `db_backoffice` in `infra/docker/mysql/init/00-create-schemas.sql`
- [x] T008 [P] Commit Prometheus scrape targets for every Spring Boot actuator and infrastructure component in `infra/prometheus/prometheus.yml`
- [x] T009 [P] Commit Grafana datasource and dashboard provisioning manifests in `infra/grafana/provisioning/datasources/prometheus.yml` and `infra/grafana/provisioning/dashboards/dashboards.yml`
- [x] T010 Create the shared observability starter for `trace_id` creation, MDC enrichment, structured JSON logging, and HTTP/RabbitMQ/Kafka header propagation in `libs/observability-starter/src/main/java/com/fabriciosanches/adamodulo3/observability/TraceIdFilter.java`, `libs/observability-starter/src/main/java/com/fabriciosanches/adamodulo3/observability/MdcEnricher.java`, and `libs/observability-starter/src/main/java/com/fabriciosanches/adamodulo3/observability/MessagingTracePropagator.java`
- [x] T011 Create reusable OpenAPI/AsyncAPI drift assertions and PACT helpers in `libs/contract-test-kit/src/main/java/com/fabriciosanches/adamodulo3/contracttest/OpenApiContractVerifier.java`, `libs/contract-test-kit/src/main/java/com/fabriciosanches/adamodulo3/contracttest/AsyncApiContractVerifier.java`, and `libs/contract-test-kit/src/main/java/com/fabriciosanches/adamodulo3/contracttest/PactSupport.java`
- [x] T012 Create the shared Testcontainers environment for MySQL, Redis, RabbitMQ, and Kafka in `libs/testcontainers-support/src/test/java/com/fabriciosanches/adamodulo3/testcontainers/IntegrationEnvironment.java`

**Checkpoint**: Contracts, infra, trace tooling, and quality gates exist before any service-specific code begins.

---

## Phase 2: Gateway + Auth + Faturas

**Purpose**: Deliver the trusted entry point and the first business bounded context so authenticated clients can create, query, retry, and escalate faturas with constitution-compliant observability and retry ceilings.

### User Story 1 - Authenticate and propagate identity (Priority: P1)

**Goal**: Issue a 20-minute HMAC-SHA256 JWT from the API Gateway and propagate `Authorization: Bearer` plus `trace_id` to downstream services.

**Independent Test**: Authenticate through `POST /api/v1/auth/login`, confirm the `Authorization` response header is returned, and verify the gateway forwards JWT and `trace_id` to the downstream faturas route while invalid JWTs are rejected.

### Tests for User Story 1 (MANDATORY)

- [ ] T013 [P] [US1] Add OpenAPI contract validation for `POST /api/v1/auth/login` response headers and error cases in `apps/api-gateway/src/test/java/com/fabriciosanches/adamodulo3/apigateway/contract/AuthLoginOpenApiContractTest.java`
- [ ] T014 [P] [US1] Add HTTP PACT provider verification for gateway login and JWT header semantics in `apps/api-gateway/src/test/java/com/fabriciosanches/adamodulo3/apigateway/pact/AuthLoginPactProviderTest.java`
- [ ] T015 [P] [US1] Add Testcontainers-backed integration coverage for JWT issuance, downstream propagation, and invalid-token rejection in `apps/api-gateway/src/test/java/com/fabriciosanches/adamodulo3/apigateway/integration/AuthGatewayIntegrationTest.java`
- [ ] T064 [P] [US1] Add unit tests for the login HTTP endpoint and gateway handlers, including `AuthController`, `JwtTokenService`, `BCryptPasswordVerifier`, and `TracePropagationGatewayFilter`, in `apps/api-gateway/src/test/java/com/fabriciosanches/adamodulo3/apigateway/unit/AuthControllerTest.java`, `apps/api-gateway/src/test/java/com/fabriciosanches/adamodulo3/apigateway/unit/JwtTokenServiceTest.java`, `apps/api-gateway/src/test/java/com/fabriciosanches/adamodulo3/apigateway/unit/BCryptPasswordVerifierTest.java`, and `apps/api-gateway/src/test/java/com/fabriciosanches/adamodulo3/apigateway/unit/TracePropagationGatewayFilterTest.java`

### Implementation for User Story 1

- [ ] T016 [US1] Add Flyway auth schema creation and BCrypt-only seed users in `apps/api-gateway/src/main/resources/db/migration/db_auth/V1__create_auth_user.sql` and `apps/api-gateway/src/main/resources/db/migration/db_auth/V2__seed_auth_users.sql`
- [ ] T017 [P] [US1] Implement auth persistence and BCrypt credential verification adapters in `apps/api-gateway/src/main/java/com/fabriciosanches/adamodulo3/apigateway/adapter/out/security/AuthUserEntity.java`, `apps/api-gateway/src/main/java/com/fabriciosanches/adamodulo3/apigateway/adapter/out/security/AuthUserRepository.java`, and `apps/api-gateway/src/main/java/com/fabriciosanches/adamodulo3/apigateway/adapter/out/security/BCryptPasswordVerifier.java`
- [ ] T018 [P] [US1] Implement HMAC-SHA256 JWT issuance with 20-minute TTL in `apps/api-gateway/src/main/java/com/fabriciosanches/adamodulo3/apigateway/adapter/out/token/JwtTokenService.java`
- [ ] T019 [US1] Implement the login use case and OpenAPI-aligned controller in `apps/api-gateway/src/main/java/com/fabriciosanches/adamodulo3/apigateway/application/LoginUseCase.java` and `apps/api-gateway/src/main/java/com/fabriciosanches/adamodulo3/apigateway/adapter/in/web/AuthController.java`
- [ ] T020 [US1] Implement gateway security, JWT propagation, and `trace_id` propagation using the shared starter in `apps/api-gateway/src/main/java/com/fabriciosanches/adamodulo3/apigateway/config/SecurityConfig.java` and `apps/api-gateway/src/main/java/com/fabriciosanches/adamodulo3/apigateway/config/TracePropagationGatewayFilter.java`
- [ ] T065 [US1] Add gateway and platform observability metrics for login flow, token propagation failures, and trace propagation failures in `apps/api-gateway/src/main/java/com/fabriciosanches/adamodulo3/apigateway/config/ObservabilityConfig.java`

### User Story 2 - Create, query, and retry faturas (Priority: P1)

**Goal**: Persist faturas in MySQL, serve hot reads from Redis with fallback to MySQL, dispatch `PAGAR`, and enforce the retry ceiling of 3 with `PROBLEMA` fallback plus Backoffice routing.

**Independent Test**: Create a batch, request payment for one fatura, read it back through Redis/MySQL fallback, and verify that repeated `RECUSADO` retries stop after the third attempt, move the fatura to `PROBLEMA`, and emit exactly one Backoffice routing message with persisted intake data.

### Tests for User Story 2 (MANDATORY)

- [ ] T021 [P] [US2] Add OpenAPI contract validation for `POST /api/v1/faturas/lote`, `POST /api/v1/faturas/{id}/pagamentos`, and `GET /api/v1/faturas/{id}` in `apps/ms-faturas/src/test/java/com/fabriciosanches/adamodulo3/faturas/contract/FaturasOpenApiContractTest.java`
- [ ] T022 [P] [US2] Add HTTP PACT provider coverage for the faturas endpoints and messaging PACT coverage for the `PAGAR` event in `apps/ms-faturas/src/test/java/com/fabriciosanches/adamodulo3/faturas/pact/FaturasHttpPactProviderTest.java` and `apps/ms-faturas/src/test/java/com/fabriciosanches/adamodulo3/faturas/pact/PagarEventMessagePactTest.java`
- [ ] T023 [P] [US2] Add Testcontainers-backed integration coverage for MySQL truth, Redis fallback, JWT validation, retry ceiling 3, and `PROBLEMA` transition in `apps/ms-faturas/src/test/java/com/fabriciosanches/adamodulo3/faturas/integration/FaturasIntegrationTest.java`
- [ ] T075 [P] [US2] Add Testcontainers-backed integration coverage proving the retry scheduler creates `trace_id` when missing and propagates it to Backoffice routing message headers in `apps/ms-faturas/src/test/java/com/fabriciosanches/adamodulo3/faturas/integration/FaturaRetrySchedulerTraceIdIntegrationTest.java`
- [ ] T066 [P] [US2] Add unit tests for the faturas HTTP endpoints and supporting handlers, including `FaturasController`, fatura status transitions, retry ceiling logic, and cache-aside fallback policy, in `apps/ms-faturas/src/test/java/com/fabriciosanches/adamodulo3/faturas/unit/FaturasControllerTest.java`, `apps/ms-faturas/src/test/java/com/fabriciosanches/adamodulo3/faturas/unit/FaturaRetryPolicyTest.java`, `apps/ms-faturas/src/test/java/com/fabriciosanches/adamodulo3/faturas/unit/FaturaStatusTransitionTest.java`, and `apps/ms-faturas/src/test/java/com/fabriciosanches/adamodulo3/faturas/unit/GetFaturaUseCaseTest.java`

### Implementation for User Story 2

- [ ] T024 [US2] Add Flyway migrations for `fatura` and `fatura_evento` plus `PENDENTE` seed data in `apps/ms-faturas/src/main/resources/db/migration/db_faturas/V1__create_fatura_tables.sql` and `apps/ms-faturas/src/main/resources/db/migration/db_faturas/V2__seed_faturas.sql`
- [ ] T025 [P] [US2] Implement the fatura aggregate, status transition policy, and hard retry ceiling of 3 in `apps/ms-faturas/src/main/java/com/fabriciosanches/adamodulo3/faturas/domain/Fatura.java`, `apps/ms-faturas/src/main/java/com/fabriciosanches/adamodulo3/faturas/domain/FaturaStatus.java`, and `apps/ms-faturas/src/main/java/com/fabriciosanches/adamodulo3/faturas/domain/FaturaRetryPolicy.java`
- [ ] T026 [P] [US2] Implement MySQL and Redis adapters for `db_faturas` truth plus the `faturas:{id}:status` and `faturas:{id}:snapshot` keyspaces with 24-hour TTL in `apps/ms-faturas/src/main/java/com/fabriciosanches/adamodulo3/faturas/adapter/out/persistence/mysql/` and `apps/ms-faturas/src/main/java/com/fabriciosanches/adamodulo3/faturas/adapter/out/persistence/redis/`
- [ ] T027 [US2] Implement lote creation, cache-aside GET, and payment request application services in `apps/ms-faturas/src/main/java/com/fabriciosanches/adamodulo3/faturas/application/CreateLoteUseCase.java`, `apps/ms-faturas/src/main/java/com/fabriciosanches/adamodulo3/faturas/application/GetFaturaUseCase.java`, and `apps/ms-faturas/src/main/java/com/fabriciosanches/adamodulo3/faturas/application/SolicitarPagamentoUseCase.java`
- [ ] T028 [US2] Implement OpenAPI-aligned controllers and downstream JWT validation in `apps/ms-faturas/src/main/java/com/fabriciosanches/adamodulo3/faturas/adapter/in/web/FaturasController.java` and `apps/ms-faturas/src/main/java/com/fabriciosanches/adamodulo3/faturas/config/SecurityConfig.java`
- [ ] T029 [US2] Implement the Kafka producer for `PAGAR` with `trace_id` and `authorization_subject` propagation from `.specs/asyncapi/pagar-event.yaml` in `apps/ms-faturas/src/main/java/com/fabriciosanches/adamodulo3/faturas/adapter/out/messaging/PagarEventPublisher.java`
- [ ] T030 [US2] Implement the 2-minute `RECUSADO` reprocess scheduler that stops after 3 attempts and moves the fatura to `PROBLEMA` in `apps/ms-faturas/src/main/java/com/fabriciosanches/adamodulo3/faturas/adapter/in/scheduler/FaturaRetryScheduler.java`
- [ ] T031 [US2] Add faturas Micrometer counters, MDC identifiers, and structured JSON log hooks for cache misses, retries, and `PROBLEMA` transitions in `apps/ms-faturas/src/main/java/com/fabriciosanches/adamodulo3/faturas/config/ObservabilityConfig.java`
- [ ] T057 [US2] Add the `problema_fatura` Flyway migration and persistence adapters for manual handling in `apps/ms-backoffice/src/main/resources/db/migration/db_backoffice/V1__create_problema_fatura_table.sql` and `apps/ms-backoffice/src/main/java/com/fabriciosanches/adamodulo3/backoffice/adapter/out/persistence/mysql/`
- [ ] T058 [US2] Implement exhausted-fatura routing from `ms-faturas` into backoffice and the backoffice ingestion flow in `apps/ms-faturas/src/main/java/com/fabriciosanches/adamodulo3/faturas/adapter/out/messaging/ProblemaFaturaPublisher.java`, `apps/ms-backoffice/src/main/java/com/fabriciosanches/adamodulo3/backoffice/adapter/in/messaging/ProblemaFaturaConsumer.java`, and `apps/ms-backoffice/src/main/java/com/fabriciosanches/adamodulo3/backoffice/application/RegisterProblemaFaturaUseCase.java`
- [ ] T070 [P] [US2] Add AsyncAPI contract validation and messaging PACT coverage for the Backoffice problem-routing event in `apps/ms-backoffice/src/test/java/com/fabriciosanches/adamodulo3/backoffice/contract/ProblemaFaturaRoutingContractTest.java` and `apps/ms-backoffice/src/test/java/com/fabriciosanches/adamodulo3/backoffice/pact/ProblemaFaturaRoutingPactTest.java`
- [ ] T071 [P] [US2] Add unit tests for the Backoffice messaging handler and exhausted-fatura registration, including `ProblemaFaturaConsumer`, intake mapping, and registration orchestration in `apps/ms-backoffice/src/test/java/com/fabriciosanches/adamodulo3/backoffice/unit/RegisterProblemaFaturaUseCaseTest.java` and `apps/ms-backoffice/src/test/java/com/fabriciosanches/adamodulo3/backoffice/unit/ProblemaFaturaConsumerTest.java`
- [ ] T072 [US2] Add `backoffice_problem_routes_total` metric emission, MDC enrichment, and structured log correlation when exhausted faturas are routed and registered in `apps/ms-faturas/src/main/java/com/fabriciosanches/adamodulo3/faturas/adapter/out/messaging/ProblemaFaturaPublisher.java` and `apps/ms-backoffice/src/main/java/com/fabriciosanches/adamodulo3/backoffice/config/ObservabilityConfig.java`
- [ ] T073 [P] [US2] Add Testcontainers-backed integration coverage for the `ProblemaFaturaPublisher -> ProblemaFaturaConsumer -> RegisterProblemaFaturaUseCase` flow, including payload persistence, `trace_id` propagation, and `backoffice_problem_routes_total` assertions in `apps/ms-backoffice/src/test/java/com/fabriciosanches/adamodulo3/backoffice/integration/ProblemaFaturaRoutingIntegrationTest.java`
- [ ] T074 [P] [US2] Add an exact-once routing assertion for exhausted faturas so a single exhausted lifecycle cannot emit duplicate Backoffice routing messages in `apps/ms-faturas/src/test/java/com/fabriciosanches/adamodulo3/faturas/integration/ProblemaFaturaRoutingIdempotencyTest.java` or `apps/ms-backoffice/src/test/java/com/fabriciosanches/adamodulo3/backoffice/integration/ProblemaFaturaRoutingIdempotencyTest.java`

**Checkpoint**: Authenticated clients can enter through the gateway and execute the full faturas flow with Redis/MySQL behavior, JWT validation, retry ceiling enforcement, and Backoffice escalation for exhausted retries.

---

## Phase 3: Comprovantes + Pagamentos + Active Contracts

**Purpose**: Deliver asynchronous receipt processing and the SAGA finality rule so `PAGO` is never persisted before successful consumption of `comprovante.gerado.topic`.

### User Story 4 - Accept, persist, and retrieve comprovantes (Priority: P2)

**Goal**: Accept standard PDF payloads with `202 Accepted`, persist full JSON payloads, publish receipt work asynchronously through RabbitMQ, and serve cache-aside receipt reads from Redis/MySQL.

**Independent Test**: Submit `POST /api/v1/comprovantes`, observe the `202 Accepted` UUID response and RabbitMQ queueing, then retrieve the same receipt through `GET /api/v1/comprovantes/{id}` with cache hits, fallback, and 3-attempt `404` behavior.

### Tests for User Story 4 (MANDATORY)

- [ ] T032 [P] [US4] Add OpenAPI and AsyncAPI contract validation for `POST /api/v1/comprovantes`, `GET /api/v1/comprovantes/{id}`, and `ComprovanteQueueMessage` as declared in `.specs/asyncapi/comprovante-gerado.yaml` in `apps/ms-comprovantes/src/test/java/com/fabriciosanches/adamodulo3/comprovantes/contract/ComprovantesContractTest.java`
- [ ] T033 [P] [US4] Add HTTP PACT coverage for `POST /api/v1/comprovantes` and `GET /api/v1/comprovantes/{id}` plus messaging PACT coverage for RabbitMQ receipt publication and Kafka `comprovante.gerado.topic` publication in `apps/ms-comprovantes/src/test/java/com/fabriciosanches/adamodulo3/comprovantes/pact/ComprovantesHttpPactProviderTest.java` and `apps/ms-comprovantes/src/test/java/com/fabriciosanches/adamodulo3/comprovantes/pact/ComprovantesMessagingPactTest.java`
- [ ] T034 [P] [US4] Add Testcontainers-backed integration coverage for `202 Accepted`, JSON payload retention, Redis 24-hour cache, and the 3-attempt `404` ceiling in `apps/ms-comprovantes/src/test/java/com/fabriciosanches/adamodulo3/comprovantes/integration/ComprovantesIntegrationTest.java`
- [ ] T067 [P] [US4] Add unit tests for the comprovantes HTTP endpoints and messaging handler, including `ComprovantesController`, `ComprovanteQueueConsumer`, UUID generation, cache-aside retry policy, and full JSON payload retention mapping in `apps/ms-comprovantes/src/test/java/com/fabriciosanches/adamodulo3/comprovantes/unit/ComprovantesControllerTest.java`, `apps/ms-comprovantes/src/test/java/com/fabriciosanches/adamodulo3/comprovantes/unit/ComprovanteQueueConsumerTest.java`, `apps/ms-comprovantes/src/test/java/com/fabriciosanches/adamodulo3/comprovantes/unit/ComprovanteLookupPolicyTest.java`, `apps/ms-comprovantes/src/test/java/com/fabriciosanches/adamodulo3/comprovantes/unit/ComprovanteFactoryTest.java`, and `apps/ms-comprovantes/src/test/java/com/fabriciosanches/adamodulo3/comprovantes/unit/ComprovantePayloadMappingTest.java`

### Implementation for User Story 4

- [ ] T035 [US4] Add the comprovante Flyway migration with `payload_pdf_json` and `payload_notificacao_completo` JSON columns in `apps/ms-comprovantes/src/main/resources/db/migration/db_comprovantes/V1__create_comprovante_table.sql`
- [ ] T036 [P] [US4] Implement the comprovante aggregate, UUID v4 generation rule, and cache-aside retry policy in `apps/ms-comprovantes/src/main/java/com/fabriciosanches/adamodulo3/comprovantes/domain/Comprovante.java`, `apps/ms-comprovantes/src/main/java/com/fabriciosanches/adamodulo3/comprovantes/domain/ComprovanteStatus.java`, and `apps/ms-comprovantes/src/main/java/com/fabriciosanches/adamodulo3/comprovantes/domain/ComprovanteLookupPolicy.java`
- [ ] T037 [P] [US4] Implement MySQL and Redis adapters for durable retention plus the `comprovantes:{id}` keyspace with 24-hour TTL in `apps/ms-comprovantes/src/main/java/com/fabriciosanches/adamodulo3/comprovantes/adapter/out/persistence/mysql/` and `apps/ms-comprovantes/src/main/java/com/fabriciosanches/adamodulo3/comprovantes/adapter/out/persistence/redis/`
- [ ] T038 [US4] Implement the OpenAPI-aligned `202 Accepted` POST and cache-aside GET controllers in `apps/ms-comprovantes/src/main/java/com/fabriciosanches/adamodulo3/comprovantes/adapter/in/web/ComprovantesController.java`
- [ ] T039 [US4] Implement RabbitMQ Direct exchange publication and consumption of `ComprovanteQueueMessage` with `trace_id` propagation in `apps/ms-comprovantes/src/main/java/com/fabriciosanches/adamodulo3/comprovantes/adapter/out/messaging/ComprovanteQueuePublisher.java` and `apps/ms-comprovantes/src/main/java/com/fabriciosanches/adamodulo3/comprovantes/adapter/in/messaging/ComprovanteQueueConsumer.java`
- [ ] T040 [US4] Implement Kafka publication of `comprovante.gerado.topic` only after MySQL persistence of the complete JSON payload in `apps/ms-comprovantes/src/main/java/com/fabriciosanches/adamodulo3/comprovantes/adapter/out/messaging/ComprovanteGeradoPublisher.java`
- [ ] T041 [US4] Add comprovantes metrics, MDC identifiers, and structured JSON logging for accepted posts, queue publications, consumer failures, cache hits, cache misses, and GET retries in `apps/ms-comprovantes/src/main/java/com/fabriciosanches/adamodulo3/comprovantes/config/ObservabilityConfig.java`

### User Story 3 - Execute payment saga and finalize only after comprovante confirmation (Priority: P1)

**Goal**: Consume `PAGAR`, apply the mock gateway rules, compensate `RECUSADO` cases, and persist `PAGO` only after successful asynchronous confirmation from `comprovante.gerado.topic`.

**Independent Test**: Publish a `PAGAR` event, simulate gateway outcomes `0`, `4`, `9` and a success case, then verify `PAGO` is blocked until the receipt confirmation arrives on Kafka.

### Tests for User Story 3 (MANDATORY)

- [ ] T042 [P] [US3] Add OpenAPI and AsyncAPI contract validation for `POST /api/v1/pagamentos/mock/gateway/lote`, `PAGAR` consumption, and `comprovante.gerado.topic` confirmation handling in `apps/ms-pagamentos/src/test/java/com/fabriciosanches/adamodulo3/pagamentos/contract/PagamentosContractTest.java`
- [ ] T043 [P] [US3] Add HTTP PACT coverage for `POST /api/v1/pagamentos/mock/gateway/lote` plus messaging PACT coverage proving that synchronous `PAGO` finalization is forbidden before `comprovante.gerado.topic` and that compensation payloads stay contract-compliant in `apps/ms-pagamentos/src/test/java/com/fabriciosanches/adamodulo3/pagamentos/pact/PagamentosHttpPactProviderTest.java` and `apps/ms-pagamentos/src/test/java/com/fabriciosanches/adamodulo3/pagamentos/pact/PagamentosMessagingPactTest.java`
- [ ] T044 [P] [US3] Add Testcontainers-backed integration coverage for `PROCESSANDO`, `RECUSADO`, compensation, and async-only `PAGO` finalization in `apps/ms-pagamentos/src/test/java/com/fabriciosanches/adamodulo3/pagamentos/integration/PagamentosIntegrationTest.java`
- [ ] T068 [P] [US3] Add unit tests for the pagamentos HTTP endpoint and messaging handlers, including `PagamentosMockGatewayController`, `PagarEventConsumer`, `ComprovanteGeradoConsumer`, mock gateway decision rules, compensation policy, and payment finality guard in `apps/ms-pagamentos/src/test/java/com/fabriciosanches/adamodulo3/pagamentos/unit/PagamentosMockGatewayControllerTest.java`, `apps/ms-pagamentos/src/test/java/com/fabriciosanches/adamodulo3/pagamentos/unit/PagarEventConsumerTest.java`, `apps/ms-pagamentos/src/test/java/com/fabriciosanches/adamodulo3/pagamentos/unit/ComprovanteGeradoConsumerTest.java`, `apps/ms-pagamentos/src/test/java/com/fabriciosanches/adamodulo3/pagamentos/unit/MockGatewayClientTest.java`, `apps/ms-pagamentos/src/test/java/com/fabriciosanches/adamodulo3/pagamentos/unit/PagamentoFinalityPolicyTest.java`, and `apps/ms-pagamentos/src/test/java/com/fabriciosanches/adamodulo3/pagamentos/unit/ProcessPagamentoUseCaseTest.java`

### Implementation for User Story 3

- [ ] T045 [US3] Add the pagamento Flyway migration in `apps/ms-pagamentos/src/main/resources/db/migration/db_pagamentos/V1__create_pagamento_table.sql`
- [ ] T046 [P] [US3] Implement the pagamento aggregate and the policy that forbids `PAGO` before Kafka confirmation consumption in `apps/ms-pagamentos/src/main/java/com/fabriciosanches/adamodulo3/pagamentos/domain/Pagamento.java`, `apps/ms-pagamentos/src/main/java/com/fabriciosanches/adamodulo3/pagamentos/domain/PagamentoStatus.java`, and `apps/ms-pagamentos/src/main/java/com/fabriciosanches/adamodulo3/pagamentos/domain/PagamentoFinalityPolicy.java`
- [ ] T047 [P] [US3] Implement Kafka consumers for `PAGAR` and `comprovante.gerado.topic` with `trace_id`, JWT, and business-correlation header handling in `apps/ms-pagamentos/src/main/java/com/fabriciosanches/adamodulo3/pagamentos/adapter/in/messaging/PagarEventConsumer.java` and `apps/ms-pagamentos/src/main/java/com/fabriciosanches/adamodulo3/pagamentos/adapter/in/messaging/ComprovanteGeradoConsumer.java`
- [ ] T048 [US3] Implement the mock gateway decision flow, compensation handling, and `0`/`4`/`9` refusal rule in `apps/ms-pagamentos/src/main/java/com/fabriciosanches/adamodulo3/pagamentos/adapter/out/gatewaymock/MockGatewayClient.java`, `apps/ms-pagamentos/src/main/java/com/fabriciosanches/adamodulo3/pagamentos/application/ProcessPagamentoUseCase.java`, and `apps/ms-pagamentos/src/main/java/com/fabriciosanches/adamodulo3/pagamentos/application/CompensatePagamentoUseCase.java`
- [ ] T049 [US3] Implement the OpenAPI-aligned mock gateway controller and MySQL persistence adapters in `apps/ms-pagamentos/src/main/java/com/fabriciosanches/adamodulo3/pagamentos/adapter/in/web/PagamentosMockGatewayController.java` and `apps/ms-pagamentos/src/main/java/com/fabriciosanches/adamodulo3/pagamentos/adapter/out/persistence/mysql/`
- [ ] T050 [US3] Activate provider-side OpenAPI/AsyncAPI verification for pagamentos and comprovantes against `.specs/openapi/ms-pagamentos.yaml`, `.specs/openapi/ms-comprovantes.yaml`, and `.specs/asyncapi/comprovante-gerado.yaml` in `apps/ms-pagamentos/pom.xml` and `apps/ms-comprovantes/pom.xml`
- [ ] T051 [US3] Add pagamentos metrics, MDC identifiers, and structured JSON logging for blocked synchronous `PAGO`, compensations, `PAGAR` consumption, and confirmed `PAGO` transitions in `apps/ms-pagamentos/src/main/java/com/fabriciosanches/adamodulo3/pagamentos/config/ObservabilityConfig.java`

**Checkpoint**: Receipt processing and payment finality are both live, and the repository enforces the rule that `PAGO` cannot be written before `comprovante.gerado.topic` is consumed successfully.

---

## Phase 4: Alerts, Operational Visibility, and Automated Delivery

**Purpose**: Complete downstream notification handling, observability dashboards, and delivery automation for the full monorepo.

### User Story 5 - Deliver receipt events to notifications (Priority: P3)

**Goal**: Consume `comprovante.gerado.topic`, keep the full JSON payload intact, retry at most 3 times, and route exhausted failures to `comprovante.gerado.DLT`.

**Independent Test**: Publish a `comprovante.gerado.topic` message, force transient failures, and verify three retries maximum before the final message lands in `comprovante.gerado.DLT` with the expected failure metadata.

### Tests for User Story 5 (MANDATORY)

- [ ] T052 [P] [US5] Add AsyncAPI contract validation and messaging PACT coverage for notification consumption plus DLT routing in `apps/ms-notificacoes/src/test/java/com/fabriciosanches/adamodulo3/notificacoes/contract/NotificacoesContractTest.java` and `apps/ms-notificacoes/src/test/java/com/fabriciosanches/adamodulo3/notificacoes/pact/NotificacoesMessagingPactTest.java`
- [ ] T053 [P] [US5] Add Testcontainers-backed integration coverage for the 3-attempt retry ceiling and `comprovante.gerado.DLT` handling in `apps/ms-notificacoes/src/test/java/com/fabriciosanches/adamodulo3/notificacoes/integration/NotificacoesIntegrationTest.java`
- [ ] T069 [P] [US5] Add unit tests for the notification messaging handler and downstream orchestration, including `ComprovanteGeradoListener`, notification retry policy, DLT metadata mapping, and outbound notification orchestration in `apps/ms-notificacoes/src/test/java/com/fabriciosanches/adamodulo3/notificacoes/unit/ComprovanteGeradoListenerTest.java`, `apps/ms-notificacoes/src/test/java/com/fabriciosanches/adamodulo3/notificacoes/unit/NotificationRetryPolicyTest.java`, `apps/ms-notificacoes/src/test/java/com/fabriciosanches/adamodulo3/notificacoes/unit/ComprovanteGeradoDltMappingTest.java`, and `apps/ms-notificacoes/src/test/java/com/fabriciosanches/adamodulo3/notificacoes/unit/ProcessNotificacaoUseCaseTest.java`

### Implementation for User Story 5

- [ ] T054 [US5] Implement the Kafka listener with `@RetryableTopic`, attempt ceiling 3, DLT routing, full JSON payload consumption, and `trace_id`/MDC propagation in `apps/ms-notificacoes/src/main/java/com/fabriciosanches/adamodulo3/notificacoes/adapter/in/messaging/ComprovanteGeradoListener.java`
- [ ] T055 [US5] Implement notification application services and outbound delivery adapters in `apps/ms-notificacoes/src/main/java/com/fabriciosanches/adamodulo3/notificacoes/application/ProcessNotificacaoUseCase.java` and `apps/ms-notificacoes/src/main/java/com/fabriciosanches/adamodulo3/notificacoes/adapter/out/notification/`
- [ ] T056 [US5] Add notificacoes Micrometer counters and structured JSON logging for consumer throughput, retries, and DLT volume in `apps/ms-notificacoes/src/main/java/com/fabriciosanches/adamodulo3/notificacoes/config/ObservabilityConfig.java`

### Cross-Cutting Delivery and Operations

- [ ] T059 [P] Commit Grafana dashboards for gateway auth, faturas lifecycle, pagamentos saga, comprovantes throughput, notificacoes DLT, and trace correlation in `infra/grafana/dashboards/gateway-auth-overview.json`, `infra/grafana/dashboards/faturas-lifecycle.json`, `infra/grafana/dashboards/pagamentos-saga.json`, `infra/grafana/dashboards/comprovantes-throughput.json`, `infra/grafana/dashboards/notificacoes-dlt.json`, and `infra/grafana/dashboards/trace-correlation.json`
- [ ] T060 [P] Wire actuator Prometheus exposure and shared observability starter consumption in `apps/api-gateway/src/main/resources/application.properties`, `apps/ms-faturas/src/main/resources/application.properties`, `apps/ms-pagamentos/src/main/resources/application.properties`, `apps/ms-comprovantes/src/main/resources/application.properties`, `apps/ms-notificacoes/src/main/resources/application.properties`, and `apps/ms-backoffice/src/main/resources/application.properties`
- [ ] T061 Configure GitHub Actions for Maven `verify`, OpenAPI/AsyncAPI drift checks, HTTP and messaging PACT verification, Testcontainers integration suites, Jacoco `>= 80%`, and multi-stage Docker image builds in `.github/workflows/ci.yml`
- [ ] T062 Add repository-wide Jacoco aggregation, contract verification, and fail-fast quality gates in `pom.xml` and `libs/contract-test-kit/pom.xml`
- [ ] T063 Add an end-to-end payment saga regression suite covering JWT propagation, no synchronous `PAGO`, JSON payload retention, RabbitMQ/Kafka wiring, retry ceilings, and DLT handling in `apps/api-gateway/src/test/java/com/fabriciosanches/adamodulo3/apigateway/e2e/PaymentSagaEndToEndTest.java`

**Checkpoint**: The full platform is operational, observable, contract-gated, and automated in CI/CD with downstream failure handling and manual backoffice capture.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1**: No dependencies. It establishes the monorepo, contracts, infrastructure, observability starter, and shared testing support.
- **Phase 2**: Starts only after Phase 1 is complete. Gateway/auth tasks unlock protected downstream execution, and faturas plus exhausted-retry Backoffice routing depend on those auth and contract assets.
- **Phase 3**: Starts only after Phase 2 is complete. Comprovantes and pagamentos are delivered in this phase because payment finality depends on the receipt confirmation channel being active.
- **Phase 4**: Starts only after Phase 3 is complete. Notifications, dashboards, and CI/CD are the final operationalization layer.

### User Story Dependencies

- **US1**: Depends on Phase 1 only.
- **US2**: Depends on US1 security and propagation being available in Phase 2, plus the Phase 1 AsyncAPI contract for exhausted-retry Backoffice routing.
- **US4**: Depends on Phase 1 shared infra and contracts and on JWT propagation patterns established in Phase 2.
- **US3**: Depends on US2 because it consumes `PAGAR`, and depends on US4 because `PAGO` finality requires `comprovante.gerado.topic`.
- **US5**: Depends on US4 because it consumes the published receipt-confirmation stream.

### Within Each User Story

- Tests MUST be written before implementation and fail first.
- Flyway and persistence definitions precede service logic.
- Domain policies precede adapters and controllers.
- OpenAPI/AsyncAPI alignment must be preserved before a story is considered complete.
- Trace propagation, MDC logging, and metrics are part of each story's definition of done.

### Constitution Rules Enforced by This Task Plan

- `PAGO` MUST NOT be persisted synchronously before `comprovante.gerado.topic`; see T043, T044, T046, T047, T051, and T063.
- Retry ceilings are hard-capped at 3 for faturas, comprovante reads, and notifications; see T023, T025, T030, T034, T036, T053, and T054.
- BCrypt-only credentials are mandatory for auth seeds and verification; see T015, T016, and T017.
- Full JSON payload retention is mandatory for comprovantes and notification fan-out; see T034, T035, T040, and T054.
- JWT propagation and validation are mandatory across HTTP and messaging flows; see T015, T020, T023, T028, T047, and T063.
- DLT handling is mandatory for notification failures; see T052, T053, T054, and T056.

---

## Parallel Opportunities

- Phase 1 tasks marked `[P]` can be split across build, contracts, and infra contributors once T001 defines the module layout.
- In Phase 2, US1 test tasks T013-T015 can run together, and US2 test tasks T021-T023 can run together after the contracts exist.
- In Phase 3, US4 tasks T036-T037 can run in parallel, and US3 tasks T046-T047 can run in parallel once the migrations are in place.
- In Phase 4, dashboard/properties/CI tasks T059-T062 can run in parallel after the notification flow is stable.

## Parallel Example: User Story 1

```text
T013 AuthLoginOpenApiContractTest
T014 AuthLoginPactProviderTest
T015 AuthGatewayIntegrationTest
```

## Parallel Example: User Story 2

```text
T025 Fatura domain retry policy
T026 Faturas MySQL and Redis adapters
```

## Parallel Example: User Story 4

```text
T036 Comprovante domain and lookup policy
T037 Comprovante MySQL and Redis adapters
```

## Parallel Example: User Story 5

```text
T052 Notificacoes contract and PACT tests
T053 Notificacoes integration retry and DLT test
```

---

## Implementation Strategy

### MVP First

1. Complete Phase 1.
2. Complete US1 and US2 in Phase 2.
3. Validate the authenticated faturas flow independently before proceeding.

### Incremental Delivery

1. Phase 1 establishes the platform and quality gates.
2. Phase 2 delivers the first user-visible, independently testable value through gateway authentication, faturas processing, and exhausted-retry Backoffice escalation.
3. Phase 3 adds the asynchronous receipt and payment-finality rules without weakening the earlier contracts.
4. Phase 4 finishes downstream notification handling, dashboards, and CI/CD enforcement.

### Team Strategy

1. One stream owns parent build plus infra in Phase 1 while another authors `.specs/` contracts.
2. In Phase 2, one stream owns `apps/api-gateway` while another owns `apps/ms-faturas` plus the minimal `apps/ms-backoffice` exhausted-retry intake slice after T020 is stable.
3. In Phase 3, one stream owns `apps/ms-comprovantes` while another owns `apps/ms-pagamentos`, integrating through the approved AsyncAPI artifacts.
4. In Phase 4, one stream owns `apps/ms-notificacoes` while another owns observability dashboards and `.github/workflows/ci.yml`.

## Notes

- All tasks follow the required checklist format and are chronologically ordered.
- Template placeholders have been fully removed.
- Exact paths match the planned monorepo layout under `apps/`, `libs/`, `infra/`, `.specs/`, and `.github/workflows/`.