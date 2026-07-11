# Data Model: Payment SAGA Contracts

## Overview

This feature uses a mixed persistence strategy with MySQL 8.0 as the durable source of truth, Redis as a hot-state projection/cache layer, RabbitMQ for asynchronous comprovante processing, and Kafka for event-stream confirmation and notification fan-out.

The model is split by bounded context to preserve ownership and hexagonal isolation.

## MySQL Logical Schemas

### db_auth

#### Table: auth_user

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | BIGINT | PK | Technical identifier |
| username | VARCHAR(100) | UNIQUE, NOT NULL | Login identity |
| password_hash | VARCHAR(255) | NOT NULL | BCrypt hash only |
| enabled | BOOLEAN | NOT NULL | Authentication gate |
| roles_json | JSON | NOT NULL | Granted roles/authorities |
| created_at | TIMESTAMP | NOT NULL | Audit timestamp |
| updated_at | TIMESTAMP | NOT NULL | Audit timestamp |

Rules:
- Password material is never stored in plaintext.
- Seed data is generated with BCrypt before insertion.

### db_faturas

#### Table: fatura

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | CHAR(36) | PK | UUID business identifier |
| lote_id | CHAR(36) | NOT NULL | Batch correlation |
| cliente_documento | VARCHAR(32) | NOT NULL | Customer identifier |
| valor_total | DECIMAL(19,2) | NOT NULL | Monetary amount |
| status | VARCHAR(20) | NOT NULL | `PENDENTE`, `SOLICITADO`, `RECUSADO`, `PROBLEMA` |
| retry_count | INT | NOT NULL DEFAULT 0 | Strict ceiling of 3 |
| trace_id_origem | CHAR(36) | NULL | Initial trace correlation |
| created_at | TIMESTAMP | NOT NULL | Creation audit |
| updated_at | TIMESTAMP | NOT NULL | Last state update |

#### Table: fatura_evento

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | BIGINT | PK | Event row identifier |
| fatura_id | CHAR(36) | FK -> fatura.id | Aggregate link |
| tipo_evento | VARCHAR(50) | NOT NULL | `LOTE_CRIADO`, `PAGAR_DISPARADO`, `RECUSADO_REPROCESSADO`, `PROBLEMA_ENCAMINHADO` |
| payload_json | JSON | NOT NULL | Immutable event snapshot |
| trace_id | CHAR(36) | NOT NULL | Correlation |
| created_at | TIMESTAMP | NOT NULL | Audit timestamp |

Rules:
- MySQL is the immutable truth for faturas.
- Redis may project status, but durable correction comes from MySQL.

### db_pagamentos

#### Table: pagamento

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | CHAR(36) | PK | UUID business identifier |
| fatura_id | CHAR(36) | NOT NULL | External correlation to fatura |
| gateway_lote_id | CHAR(36) | NULL | Mock gateway batch correlation |
| status | VARCHAR(20) | NOT NULL | `PROCESSANDO`, `RECUSADO`, `PAGO` |
| motivo_recusa | VARCHAR(255) | NULL | Compensation reason |
| trace_id | CHAR(36) | NOT NULL | Full-flow correlation |
| created_at | TIMESTAMP | NOT NULL | Audit timestamp |
| updated_at | TIMESTAMP | NOT NULL | Last state change |

Rules:
- `PROCESSANDO` is allowed before receipt confirmation.
- `PAGO` is forbidden until `comprovante.gerado.topic` is consumed successfully.

### db_comprovantes

#### Table: comprovante

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | CHAR(36) | PK | UUID v4 returned by `POST /api/v1/comprovantes` |
| fatura_id | CHAR(36) | NULL | Business correlation when available |
| pagamento_id | CHAR(36) | NULL | Payment correlation when available |
| status | VARCHAR(20) | NOT NULL | `RECEBIDO`, `PROCESSADO`, `ERRO` |
| payload_pdf_json | JSON | NOT NULL | Standard PDF input payload |
| payload_notificacao_completo | JSON | NOT NULL | Full downstream alert payload |
| trace_id | CHAR(36) | NOT NULL | Correlation |
| created_at | TIMESTAMP | NOT NULL | Audit timestamp |
| updated_at | TIMESTAMP | NOT NULL | Last processing update |

Rules:
- `payload_notificacao_completo` preserves 100% of downstream alert data.
- The MySQL row is the recovery source when Redis cache misses.

### db_backoffice

#### Table: problema_fatura

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | BIGINT | PK | Technical identifier |
| fatura_id | CHAR(36) | NOT NULL | Critical case reference |
| motivo | VARCHAR(255) | NOT NULL | Exhausted retry explanation |
| retry_count_final | INT | NOT NULL | Must be 3 |
| payload_contexto | JSON | NOT NULL | Operational context snapshot |
| trace_id | CHAR(36) | NOT NULL | Correlation |
| created_at | TIMESTAMP | NOT NULL | Audit timestamp |

Rules:
- Only faturas that exhaust the retry ceiling are recorded here.

## Redis Keyspaces

### ms-faturas

| Key Pattern | Type | TTL | Purpose |
|-------------|------|-----|---------|
| `faturas:{id}:status` | JSON/String | 24h | Hot projection of current status and retry count |
| `faturas:{id}:snapshot` | JSON | 24h | Read model for `GET /api/v1/faturas/{id}` |

### ms-comprovantes

| Key Pattern | Type | TTL | Purpose |
|-------------|------|-----|---------|
| `comprovantes:{id}` | JSON | 24h | Cache-aside read model for receipt lookup |

Rules:
- Redis never replaces MySQL as durable truth.
- Cache misses trigger fallback to MySQL.
- Cache retry limit is 3 attempts before `404` on comprovante reads.

## Event Payload Structures

### PAGAR event

Required fields:
- `event_id`
- `trace_id`
- `fatura_id`
- `lote_id`
- `valor_total`
- `requested_at`
- `authorization_subject`

### ComprovanteQueueMessage

Required fields:
- `comprovante_id`
- `trace_id`
- `fatura_id`
- `pagamento_id`
- `payload_pdf`
- `requested_at`

### comprovante.gerado.topic message

Required fields:
- `event_id`
- `trace_id`
- `comprovante_id`
- `fatura_id`
- `pagamento_id`
- `payload_notificacao_completo`
- `generated_at`

### comprovante.gerado.DLT message

Required fields:
- All original `comprovante.gerado.topic` fields
- `failure_reason`
- `attempt_count`
- `failed_at`

### ProblemaFatura routing message

Required fields:
- `event_id`
- `trace_id`
- `fatura_id`
- `retry_count_final`
- `motivo`
- `payload_contexto`
- `routed_at`

## State Machines

### Fatura lifecycle

`PENDENTE -> SOLICITADO -> RECUSADO -> SOLICITADO -> ... -> PROBLEMA`

Allowed transitions:
- `PENDENTE -> SOLICITADO`
- `SOLICITADO -> RECUSADO`
- `RECUSADO -> SOLICITADO` while `retry_count < 3`
- `RECUSADO -> PROBLEMA` when `retry_count = 3`

### Pagamento lifecycle

`PROCESSANDO -> RECUSADO | PAGO`

Allowed transitions:
- `PROCESSANDO -> RECUSADO` on mock rule `0`, `4`, `9`
- `PROCESSANDO -> PAGO` only after successful consumption of `comprovante.gerado.topic`

### Comprovante lifecycle

`RECEBIDO -> PROCESSADO | ERRO`

## Cross-Cutting Data Constraints

- All aggregates and events carry `trace_id`.
- Business IDs use UUIDs unless a contract artifact requires another format.
- Monetary values use fixed decimal precision and must not use floating point types.
- JSON columns store the authoritative payload snapshots required for audit and replay.