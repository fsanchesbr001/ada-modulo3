INSERT INTO db_faturas.fatura (
    id,
    lote_id,
    cliente_documento,
    valor_total,
    status,
    retry_count,
    trace_id_origem
) VALUES
    ('11111111-1111-1111-1111-111111111111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '11122233344', 125.90, 'PENDENTE', 0, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'),
    ('22222222-2222-2222-2222-222222222222', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '22233344455', 79.10, 'PENDENTE', 0, 'cccccccc-cccc-cccc-cccc-cccccccccccc'),
    ('33333333-3333-3333-3333-333333333333', 'dddddddd-dddd-dddd-dddd-dddddddddddd', '33344455566', 340.00, 'PENDENTE', 0, 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee')
ON DUPLICATE KEY UPDATE
    lote_id = VALUES(lote_id),
    cliente_documento = VALUES(cliente_documento),
    valor_total = VALUES(valor_total),
    status = VALUES(status),
    retry_count = VALUES(retry_count),
    trace_id_origem = VALUES(trace_id_origem);
