package com.fabriciosanches.adamodulo3.backoffice.adapter.out.persistence.mysql;

import org.springframework.jdbc.core.JdbcTemplate;

public class ProblemaFaturaJdbcRepository {

    private static final String INSERT_SQL = """
            INSERT INTO db_backoffice.problema_fatura
                (idempotency_key, fatura_id, motivo, retry_count_final, payload_contexto, trace_id)
            VALUES (?, ?, ?, ?, CAST(? AS JSON), ?)
            ON DUPLICATE KEY UPDATE
                id = id
            """;

    private final JdbcTemplate jdbcTemplate;

    public ProblemaFaturaJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean saveIfAbsent(ProblemaFaturaRecord record) {
        int affected = jdbcTemplate.update(
                INSERT_SQL,
                record.idempotencyKey(),
                record.faturaId(),
                record.motivo(),
                record.retryCountFinal(),
                record.payloadContexto(),
                record.traceId());
        return affected > 0;
    }
}
