<!--
Sync Impact Report
- Version change: 1.1.0 -> 1.1.1
- Modified principles:
- VII. SAGA Finality and Fail-Fast Resilience -> VII. SAGA Finality and Fail-Fast Resilience
- Added principles:
	- None
- Added sections:
	- None
- Removed sections:
	- None
- Templates requiring updates:
	- .specify/templates/plan-template.md: ✅ no changes required
	- .specify/templates/spec-template.md: ✅ no changes required
	- .specify/templates/tasks-template.md: ✅ no changes required
	- README.md: ✅ no changes required
- Deferred TODOs:
	- None
-->

# Ada Modulo3 Constitution

## Core Principles

### I. Bounded Contexts + Hexagonal Isolation
Every service MUST belong to an explicit bounded context (API Gateway,
Invoices/Faturas, Payments/SAGA, Receipts/Comprovantes, Notifications,
Backoffice) and MUST implement Hexagonal Architecture (Ports and Adapters).
Domain entities, value objects, policies, and use cases MUST remain framework-
agnostic and MUST NOT depend on Spring, JPA, Redis, Kafka, RabbitMQ, HTTP, or
adapter classes. All external dependencies MUST be accessed through inbound or
outbound ports implemented by adapters.
Rationale: strict isolation preserves domain integrity, enables independent
evolution per context, and reduces coupling across distributed services.

### II. Clean Code + SOLID + Explicit Domain Language
Code MUST follow Clean Code and SOLID principles. Names for classes, methods,
variables, events, topics, queues, and DTO fields MUST be domain-descriptive and
unambiguous. Magic numbers and magic strings are forbidden; constants and typed
value objects MUST be used. Methods MUST be small and single-purpose, and
classes MUST have one reason to change.
Rationale: clear intent and disciplined design reduce defects in financial
systems and improve maintainability for multi-team monorepo development.

### III. Test and Contract Quality Gates (NON-NEGOTIABLE)
All REST controllers and endpoints MUST have unit and integration tests.
Consumer-Driven Contract Testing with PACT is mandatory for synchronous (HTTP)
and asynchronous (messaging) integrations. CI/CD MUST enforce minimum code
coverage of 80% with Jacoco; builds below threshold MUST fail. New behavior,
changed contracts, and SAGA compensation paths MUST be covered by automated
tests before merge.
Rationale: in distributed payment flows, contract safety and regression
prevention are critical for reliability and compliance.

### IV. End-to-End Observability and Trace Propagation
A unique trace_id MUST be generated at the first entry point (API Gateway or
initial job trigger) and propagated through all HTTP headers and message headers
across the complete SAGA lifecycle. Structured JSON logging is mandatory, and
trace_id plus business identifiers (for example fatura_id) MUST be written to
MDC for every log event. Services MUST expose Micrometer/Prometheus metrics,
including business-critical and reliability signals such as SAGA compensations,
cache misses, and DLQ/DLT message counts.
Rationale: complete traceability is required to debug distributed failures,
support audits, and operate financial flows safely.

### V. Data Integrity, Messaging Resilience, and Security
Persistence MUST use Spring Data JPA with MySQL and Spring Data Redis for cache
where applicable. Database schema evolution MUST be managed exclusively by
Flyway migrations. The Receipts/Comprovantes service MUST persist the full
notification payload in a MySQL JSON column to prevent information loss during
asynchronous transitions. Messaging patterns MUST follow design intent: RabbitMQ
for high-throughput queue workloads and Kafka for event streaming. Kafka
consumers that interact with external systems MUST use @RetryableTopic with
non-blocking retries and Dead Letter handling (DLQ/DLT). Internal service calls
MUST validate JWT propagated by the API Gateway via Authorization: Bearer
headers.
Rationale: resilient messaging, controlled data evolution, and uniform token
validation are mandatory for correctness and secure service-to-service trust.

### VI. Specification-First Contracts
Controllers, DTOs, and listeners MUST be derived from or validated against the
authoritative specifications. Every HTTP endpoint MUST be validated against its
OpenAPI contract, and every message producer or consumer MUST be validated
against its AsyncAPI contract. Contract artifacts are the source of truth;
invented endpoints, fields, routing keys, topics, or listener payload shapes are
forbidden. Any contract drift MUST fail the build until the implementation and
specification are realigned.
Rationale: specification-first development prevents undocumented behavior and
keeps generated code aligned with system-level contracts.

### VII. SAGA Finality and Fail-Fast Resilience
The Payments/SAGA and Invoices/Faturas services are forbidden from marking a
transaction as PAGO synchronously after gateway mock processing. The PAGO state
MUST only be persisted after successful consumption of the Kafka topic
comprovante.gerado.topic. Any retry policy used for Redis cache-aside lookups,
Kafka @RetryableTopic consumers, or Faturas retry workers MUST cap attempts at
3. After retry exhaustion, Faturas MUST transition the transaction to PROBLEMA
and route it to Backoffice for manual management.
Rationale: payment finality must follow confirmed receipt processing, and a
strict retry ceiling prevents hidden infinite loops in critical money flows.

### VIII. BCrypt Credential Hygiene
The Auth service in the API Gateway layer and Flyway data-seeding scripts MUST
never persist or transmit plaintext passwords. BCrypt hashing is mandatory for
all password material at rest and during seed generation, and plaintext secrets
MUST NOT appear in logs, fixtures, migration files, or test data.
Rationale: password handling must be uniformly hardened across runtime and seed
paths to prevent accidental disclosure and weak credential storage.

## Engineering Baseline

- Platform baseline MUST be Java 21 and Spring Boot 3.x.
- Monorepo modules MUST expose clear ownership, bounded-context boundaries, and
	versioned contracts.
- OpenAPI and AsyncAPI artifacts MUST be treated as authoritative contract
	sources for endpoints and message flows.
- Public contracts (HTTP APIs, events, message schemas) MUST be backward-
	compatible by default; breaking changes require explicit migration plans.
- Configuration, secrets handling, and environment overrides MUST be externalized
	and MUST NOT be hardcoded.
- Any deviation from these standards MUST be documented in the feature plan with
	explicit approval rationale.

## Delivery Workflow and Enforcement Gates

- `/speckit.specify` output MUST state bounded context impact, synchronous and
	asynchronous contracts, and traceability requirements.
- `/speckit.plan` MUST include a Constitution Check section that verifies all
	eight core principles and maps them to implementation decisions.
- `/speckit.tasks` MUST include mandatory tasks for unit tests, integration
	tests, PACT contracts, OpenAPI/AsyncAPI alignment, trace propagation, metrics,
	Flyway migrations, retry ceilings, and security validation.
- Pull requests MUST include evidence for: test execution, coverage >= 80%,
	contract test status, and observability instrumentation updates.
- Code reviews MUST reject changes that leak infrastructure concerns into the
	domain layer, bypass retry/DLQ or retry-ceiling requirements, introduce PAGO
	state changes before comprovante.gerado.topic consumption, or skip JWT/
	BCrypt validation requirements.

## Governance

This constitution is authoritative for engineering decisions in this repository.
In case of conflict, this document overrides local conventions and ad hoc
practices.

Amendment process:
- Propose changes through a PR that includes motivation, impacted principles,
	migration impact, and template synchronization updates.
- Approval requires at least one Technical Lead or Principal Engineer reviewer.
- Ratification occurs on merge; LAST_AMENDED_DATE MUST be updated in ISO format.

Versioning policy:
- MAJOR: backward-incompatible governance or principle removals/redefinitions.
- MINOR: new principle/section or materially expanded mandatory guidance.
- PATCH: clarifications, wording improvements, and non-semantic refinements.

Compliance review expectations:
- Every feature PR MUST pass Constitution Check evidence in plan and tasks.
- Release readiness reviews MUST verify testing, contracts, observability,
	resilience, data migration, and JWT validation compliance.
- Non-compliant changes MUST be remediated before merge.

**Version**: 1.1.1 | **Ratified**: 2026-07-11 | **Last Amended**: 2026-07-11
