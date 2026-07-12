CREATE TABLE IF NOT EXISTS db_backoffice.problema_fatura (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    idempotency_key VARCHAR(120) NOT NULL,
    fatura_id CHAR(36) NOT NULL,
    motivo VARCHAR(255) NOT NULL,
    retry_count_final INT NOT NULL,
    payload_contexto JSON NOT NULL,
    trace_id CHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_problema_fatura_idempotency (idempotency_key),
    INDEX idx_problema_fatura_fatura_id (fatura_id),
    INDEX idx_problema_fatura_trace_id (trace_id)
);
