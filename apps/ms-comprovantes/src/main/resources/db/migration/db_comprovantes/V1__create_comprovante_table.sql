CREATE TABLE IF NOT EXISTS db_comprovantes.comprovante (
    id CHAR(36) PRIMARY KEY,
    payload_pdf_json JSON NOT NULL,
    payload_notificacao_completo JSON NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_comprovante_status ON db_comprovantes.comprovante (status);
