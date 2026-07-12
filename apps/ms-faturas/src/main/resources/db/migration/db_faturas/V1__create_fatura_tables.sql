CREATE TABLE IF NOT EXISTS db_faturas.fatura (
    id CHAR(36) PRIMARY KEY,
    lote_id CHAR(36) NOT NULL,
    cliente_documento VARCHAR(32) NOT NULL,
    valor_total DECIMAL(19,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    trace_id_origem CHAR(36) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_fatura_status_retry (status, retry_count)
);

CREATE TABLE IF NOT EXISTS db_faturas.fatura_evento (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fatura_id CHAR(36) NOT NULL,
    tipo_evento VARCHAR(50) NOT NULL,
    payload_json JSON NOT NULL,
    trace_id CHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_fatura_evento_fatura_id
        FOREIGN KEY (fatura_id) REFERENCES db_faturas.fatura (id)
);
