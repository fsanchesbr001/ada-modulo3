# Feature Specification: Payment SAGA Contracts

**Feature Branch**: `[001-payment-saga-contracts]`

**Created**: 2026-07-11

**Status**: Approved

**Input**: User description: "Process the following SDD V14 into a Spec Kit feature specification. Respect the repository constitution in .specify/memory/constitution.md and the existing templates in .specify/templates/spec-template.md and .specify/templates/tasks-template.md. Produce a complete, developer-ready Markdown specification in the standard Spec Kit structure, focused on endpoints, payloads, events, edge cases, functional requirements, success criteria, assumptions, and explicit contract requirements. Do not invent behavior outside the SDD. Defer any unspecified detail to the authoritative contract artifacts instead of guessing."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Authenticate and propagate identity (Priority: P1)

An operator or client authenticates through the API Gateway, receives a signed JWT, and that identity is propagated to downstream services for the rest of the flow.

**Why this priority**: No other service interaction is valid without a trusted entry point and a propagated token.

**Independent Test**: Can be tested by logging in through the gateway and verifying that the issued token is returned in the expected response header and forwarded to downstream calls.

**Acceptance Scenarios**:

1. **Given** valid credentials, **When** the client submits `POST /api/v1/auth/login`, **Then** the gateway returns a `Bearer` JWT signed with HMAC-SHA256 and valid for 20 minutes.
2. **Given** a request received at the gateway, **When** the gateway forwards it to another service, **Then** the `Authorization: Bearer` token is propagated downstream.
3. **Given** an internal service receives a propagated request, **When** JWT validation runs, **Then** the call is accepted only if the token is valid.

---

### User Story 2 - Create, query, and retry faturas (Priority: P1)

A financial operator creates batches of faturas, requests payment processing for a specific fatura, and retrieves the current fatura state through the fastest available source of truth.

**Why this priority**: This is the primary domain flow and the entry to the payment SAGA.

**Independent Test**: Can be tested by creating a batch, requesting payment for a fatura, and reading the same fatura back through the GET contract while exercising the Redis-to-MySQL fallback.

**Acceptance Scenarios**:

1. **Given** a valid JWT, **When** the client submits `POST /api/v1/faturas/lote`, **Then** the batch is accepted and persisted in the faturas bounded context.
2. **Given** an existing fatura, **When** the client submits `POST /api/v1/faturas/{id}/pagamentos`, **Then** the fatura is placed in `SOLICITADO` in Redis and a payment event is dispatched.
3. **Given** a fatura request and a Redis miss, **When** the client submits `GET /api/v1/faturas/{id}`, **Then** the service falls back to MySQL and returns the current state.
4. **Given** a fatura that is in `RECUSADO`, **When** the scheduled retry worker runs every 2 minutes, **Then** the retry counter increments and the flow is retried until a maximum of 3 attempts.
5. **Given** a fatura reaches the third failed retry attempt, **When** the retry ceiling is evaluated, **Then** the fatura immediately moves to `PROBLEMA` without a fourth processing pass.
6. **Given** a fatura has moved to `PROBLEMA`, **When** the critical-state routing flow runs, **Then** the case is forwarded to Backoffice for manual management through the declared asynchronous contract.

---

### User Story 3 - Execute payment saga and finalize only after comprovante confirmation (Priority: P1)

The payments service consumes the payment request event, routes it through the mock gateway rule, and only finalizes the payment as `PAGO` after receipt confirmation is received asynchronously.

**Why this priority**: Payment finality is the core integrity rule of the SAGA and must never be violated.

**Independent Test**: Can be tested by publishing a payment event, simulating the mock gateway outcome, and verifying that `PAGO` is only persisted after the downstream comprovante confirmation arrives.

**Acceptance Scenarios**:

1. **Given** a payment request event, **When** ms-pagamentos receives it, **Then** it triggers the mock gateway flow.
2. **Given** a mock gateway payment number of `0`, `4`, or `9`, **When** the flow runs, **Then** the payment is marked `RECUSADO` and compensation is executed.
3. **Given** a mock gateway payment number other than `0`, `4`, or `9`, **When** the flow runs, **Then** the payment remains `PROCESSANDO` temporarily.
4. **Given** a receipt confirmation has not yet been consumed, **When** the payment flow is still in progress, **Then** `PAGO` is not persisted.
5. **Given** a successful async confirmation from `comprovante.gerado.topic`, **When** the confirmation is consumed, **Then** the payment may be finalized as `PAGO`.

---

### User Story 4 - Accept, persist, and retrieve comprovantes (Priority: P2)

A receipt-producing client submits the standard PDF payload, receives an immediate acceptance response, and later retrieves the receipt status or content using the documented identifier.

**Why this priority**: Receipt generation is required to complete the payment SAGA and preserve all notification data.

**Independent Test**: Can be tested by sending a receipt request, observing the asynchronous queueing behavior, and then retrieving the same receipt through the GET contract.

**Acceptance Scenarios**:

1. **Given** a standard PDF payload from Module 3, **When** the client submits `POST /api/v1/comprovantes`, **Then** the service responds with `202 Accepted` and a UUID v4 identifier.
2. **Given** a submitted receipt request, **When** the service enqueues work, **Then** a `ComprovanteQueueMessage` is placed on the Direct exchange path defined by the contract.
3. **Given** a consumer receives the queued message, **When** persistence completes, **Then** the full payload is stored in MySQL in the `payload_notificacao_completo` JSON column.
4. **Given** a receipt lookup request, **When** `GET /api/v1/comprovantes/{id}` is executed, **Then** the service uses cache-aside with a 24-hour TTL and retries up to 3 attempts before returning `404`.

---

### User Story 5 - Deliver receipt events to notifications (Priority: P3)

The notifications service consumes the receipt-generated event stream and maintains retry safety and dead-letter visibility for failures.

**Why this priority**: Notifications are downstream of receipt generation and must preserve the full payload without blocking the core payment flow.

**Independent Test**: Can be tested by publishing a receipt-generated event to `comprovante.gerado.topic` and verifying the consumer, retry, and DLT behavior.

**Acceptance Scenarios**:

1. **Given** a message on `comprovante.gerado.topic`, **When** ms-notificacoes consumes it, **Then** it uses the full JSON payload from the receipts stream.
2. **Given** a temporary processing failure, **When** the message is retried, **Then** the retry policy allows up to 3 attempts.
3. **Given** all retry attempts fail, **When** the consumer exhausts its attempts, **Then** the message is routed to `comprovante.gerado.DLT`.

### Edge Cases

- A login request with invalid credentials must not return a token.
- A downstream internal call without a valid propagated JWT must be rejected by contract.
- A fatura read when Redis is empty must fall back to MySQL without changing the business state.
- A `RECUSADO` fatura must stop retrying after the third scheduled attempt and move to `PROBLEMA`.
- A payment number of `0`, `4`, or `9` must always route to `RECUSADO` in the mock gateway flow.
- A receipt request must still return `202 Accepted` even though persistence and downstream messaging are asynchronous.
- A receipt GET request must return `404` after the retry ceiling is exhausted.
- A receipt-generated message that cannot be processed after 3 attempts must appear in the DLT for analysis.
- A fatura routed to `PROBLEMA` must emit the Backoffice routing message exactly once per exhausted lifecycle.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The API Gateway MUST expose `POST /api/v1/auth/login` for username/password authentication.
- **FR-002**: Successful login MUST return a JWT in an `Authorization: Bearer` response header.
- **FR-003**: The JWT MUST use HMAC-SHA256 and MUST expire after 20 minutes.
- **FR-004**: The API Gateway global filter MUST propagate the authenticated token to downstream services.
- **FR-005**: Internal service calls MUST validate propagated JWTs before executing protected operations.
- **FR-006**: The system MUST generate a `trace_id` at the first entry point, including the gateway and initial job triggers.
- **FR-007**: The system MUST propagate `trace_id` through HTTP headers and through Kafka and RabbitMQ message headers or properties.
- **FR-008**: The system MUST write MDC entries for every log event with at least `trace_id` and one primary business identifier (`fatura_id`, `pagamento_id`, or `comprovante_id`) whenever that identifier exists in the processing context.
- **FR-009**: The system MUST emit structured JSON logs.
- **FR-010**: The system MUST expose Micrometer/Prometheus metrics for business and reliability signals, including retries, compensations, and DLT/DLQ counters.
- **FR-011**: The faturas bounded context MUST persist immutable truth in MySQL database `db_faturas`.
- **FR-012**: The faturas bounded context MUST use Redis for high-speed state handling.
- **FR-013**: The faturas service MUST expose `POST /api/v1/faturas/lote` as a JWT-protected endpoint.
- **FR-014**: The faturas service MUST expose `POST /api/v1/faturas/{id}/pagamentos` and MUST set the fatura state to `SOLICITADO` in Redis before dispatching the payment event.
- **FR-015**: The faturas service MUST expose `GET /api/v1/faturas/{id}` and MUST read through Redis with fallback to MySQL.
- **FR-016**: The faturas scheduled worker MUST run every 2 minutes to reprocess faturas in `RECUSADO`.
- **FR-017**: The faturas scheduled worker MUST increment a retry counter for each re-execution.
- **FR-018**: The faturas scheduled worker MUST stop after 3 retries and move the fatura to `PROBLEMA`.
- **FR-019**: Faturas moved to `PROBLEMA` MUST be routed to Backoffice for manual management through an asynchronous contract.
- **FR-020**: The payments service MUST consume the `PAGAR` event.
- **FR-021**: The payments service MUST expose `POST /api/v1/pagamentos/mock/gateway/lote` for mock gateway processing.
- **FR-022**: The mock gateway flow MUST route payment numbers `0`, `4`, and `9` to `RECUSADO` and compensation.
- **FR-023**: The mock gateway flow MUST leave all other payment numbers in `PROCESSANDO` only until one terminal business outcome is observed: asynchronous receipt confirmation from `comprovante.gerado.topic` (allowing `PAGO`) or a refusal/compensation path (`RECUSADO`).
- **FR-024**: The payments service MUST persist `PAGO` only after successful asynchronous confirmation from `comprovante.gerado.topic`.
- **FR-025**: The comprovantes service MUST expose `POST /api/v1/comprovantes` and MUST return `202 Accepted` with a UUID v4 identifier.
- **FR-026**: The comprovantes service MUST enqueue a `ComprovanteQueueMessage` on a Direct exchange path defined in `.specs/asyncapi/comprovante-gerado.yaml`.
- **FR-027**: The comprovantes consumer MUST persist the full payload in MySQL JSON column `payload_notificacao_completo`.
- **FR-028**: The comprovantes service MUST expose `GET /api/v1/comprovantes/{id}` using cache-aside behavior with a 24-hour TTL.
- **FR-029**: The comprovantes GET flow MUST retry up to 3 attempts before returning `404`.
- **FR-030**: The notifications service MUST consume `comprovante.gerado.topic` using the full JSON payload from the receipts stream.
- **FR-031**: The notifications consumer MUST use a retry policy of 3 attempts and route failures to `comprovante.gerado.DLT`.
- **FR-032**: Flyway seed scripts MUST store credentials in `db_auth` with BCrypt hashes only.
- **FR-033**: Flyway seed scripts MUST bulk seed `db_faturas` in `PENDENTE`.
- **FR-034**: The system MUST use OpenAPI as the authoritative source for all REST contracts.
- **FR-035**: The system MUST use AsyncAPI as the authoritative source for all messaging contracts.
- **FR-036**: All HTTP endpoints and messaging handlers MUST have unit, integration, and PACT coverage before merge.
- **FR-037**: The implementation MUST maintain code coverage at or above 80%.
- **FR-038**: Every service in scope MUST follow Hexagonal Architecture with explicit inbound and outbound ports and adapters.
- **FR-039**: Contract drift between implementation and OpenAPI or AsyncAPI MUST block delivery until realigned.

### Contract and Event Requirements *(mandatory for distributed features)*

#### Synchronous API contracts

- **api-gateway** owns `POST /api/v1/auth/login`.
- **ms-faturas** owns `POST /api/v1/faturas/lote`, `POST /api/v1/faturas/{id}/pagamentos`, and `GET /api/v1/faturas/{id}`.
- **ms-pagamentos** owns `POST /api/v1/pagamentos/mock/gateway/lote` and the listener that consumes the `PAGAR` event.
- **ms-comprovantes** owns `POST /api/v1/comprovantes` and `GET /api/v1/comprovantes/{id}`.

#### Asynchronous contracts

- **PAGAR event**: consumed by ms-pagamentos; the contract must define the payload, correlation fields, and trace propagation requirements.
- **Receipt generation stream**: the receipts flow must publish into `comprovante.gerado.topic`; ms-pagamentos depends on its successful consumption before persisting `PAGO`.
- **Notifications consumer**: ms-notificacoes consumes `comprovante.gerado.topic` with the full JSON payload and must route exhausted retries to `comprovante.gerado.DLT`.
- **Notifications dead-letter stream**: `comprovante.gerado.DLT` carries the failed notification payload plus failure metadata and is consumed for operational analysis.
- **RabbitMQ receipt work item**: ms-comprovantes must publish `ComprovanteQueueMessage` on the Direct exchange path defined in AsyncAPI.
- **Problema routing event**: ms-faturas publishes the exhausted-fatura payload to Backoffice, and ms-backoffice consumes it as the manual-management intake contract.

#### Contract test scope

- HTTP PACT coverage must validate the gateway login contract, faturas endpoints, payments mock gateway route, and comprovantes endpoints.
- Messaging PACT coverage must validate the `PAGAR` event, the receipt generation stream, the notifications consumer and DLT contracts, and the Backoffice routing contract.
- OpenAPI and AsyncAPI artifacts are the source of truth for endpoint paths, payloads, headers, events, queue semantics, and versioning expectations.

### Observability and Operations Requirements *(mandatory)*

- Every inbound HTTP request and initial scheduled trigger MUST receive a `trace_id`.
- Every outbound HTTP call and message publication MUST carry the same `trace_id`.
- Structured logging MUST be JSON and MUST follow FR-008 for mandatory MDC key requirements.
- Metrics MUST make retries, compensations, cache misses, and dead-letter events visible.
- Retry ceilings MUST be observable so operators can distinguish normal retries from exhausted retries.
- The observability contract MUST cover HTTP, Kafka, and RabbitMQ traffic uniformly.

### Key Entities *(include if feature involves data)*

- **Authentication Session**: represents a successful login and the issued JWT used for downstream propagation.
- **Fatura**: represents the immutable financial record in `db_faturas`, including state transitions such as `PENDENTE`, `SOLICITADO`, `RECUSADO`, and `PROBLEMA`.
- **Pagamento**: represents the payment lifecycle managed by ms-pagamentos, including `PROCESSANDO`, `RECUSADO`, and `PAGO`.
- **Comprovante**: represents the receipt artifact created from the standard PDF payload and retrievable by UUID.
- **Comprovante Queue Message**: represents the queued work item used to process receipt creation and downstream notification data.
- **Trace Context**: represents the `trace_id` and related correlation data that must travel through the entire flow.
- **Problema Fatura Record**: represents the exhausted retry case handed to Backoffice for manual management and audit.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of the documented REST endpoints are captured in OpenAPI and covered by contract tests before merge.
- **SC-002**: 100% of the documented async contracts are captured in AsyncAPI and covered by messaging contract tests before merge.
- **SC-003**: In end-to-end flow validation, no payment reaches `PAGO` before successful consumption of `comprovante.gerado.topic`.
- **SC-004**: Every retry-bearing flow stops at 3 attempts or fewer, and exhausted faturas are visible in `PROBLEMA` while exhausted receipt lookups return `404`.
- **SC-005**: At least 80% code coverage is maintained across the feature scope.
- **SC-006**: 100% of generated logs and published messages in the covered flows carry a `trace_id`.
- **SC-007**: 100% of seeded credentials are stored as BCrypt hashes, with no plaintext password material in seed artifacts.
- **SC-008**: 100% of faturas that exhaust the retry ceiling are routed to Backoffice with a contract-compliant payload and trace correlation.

## Assumptions

- The exact request and response field sets for each endpoint will be finalized in the authoritative OpenAPI artifacts, but the endpoint names, auth requirements, and lifecycle behavior in the SDD are fixed.
- The standard PDF payload from Module 3 is reused as the receipt input contract, and the system preserves the full payload where the SDD requires it.
- The exact queue and exchange names beyond the required Direct exchange path and the named Kafka topics will be captured in AsyncAPI.
- The gateway-issued JWT is the token used for downstream internal communication unless the contract explicitly says otherwise.
