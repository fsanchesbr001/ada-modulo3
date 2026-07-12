CREATE TABLE IF NOT EXISTS db_pagamentos.pagamento (
    id VARCHAR(36) PRIMARY KEY,
    fatura_id VARCHAR(36) NOT NULL,
    lote_id VARCHAR(36) NOT NULL,
    valor_total DECIMAL(19,2) NOT NULL,
    status VARCHAR(40) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    trace_id VARCHAR(128) NULL,
    authorization_subject VARCHAR(200) NULL,
    motivo_recusa VARCHAR(255) NULL,
    comprovante_confirmado BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_pagamento_fatura (fatura_id)
);
