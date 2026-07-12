package com.fabriciosanches.adamodulo3.pagamentos.adapter.out.persistence.mysql;

import com.fabriciosanches.adamodulo3.pagamentos.application.port.out.PagamentoRepository;
import com.fabriciosanches.adamodulo3.pagamentos.domain.Pagamento;
import com.fabriciosanches.adamodulo3.pagamentos.domain.PagamentoStatus;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;

public class JdbcPagamentoRepository implements PagamentoRepository {

    private static final String UPSERT_SQL = """
            INSERT INTO db_pagamentos.pagamento (
                id, fatura_id, lote_id, valor_total, status, retry_count, trace_id,
                authorization_subject, motivo_recusa, comprovante_confirmado
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                lote_id = VALUES(lote_id),
                valor_total = VALUES(valor_total),
                status = VALUES(status),
                retry_count = VALUES(retry_count),
                trace_id = VALUES(trace_id),
                authorization_subject = VALUES(authorization_subject),
                motivo_recusa = VALUES(motivo_recusa),
                comprovante_confirmado = VALUES(comprovante_confirmado)
            """;

    private static final String FIND_BY_ID_SQL = """
            SELECT id, fatura_id, lote_id, valor_total, status, retry_count,
                   trace_id, authorization_subject, motivo_recusa,
                   comprovante_confirmado, created_at
            FROM db_pagamentos.pagamento
            WHERE id = ?
            """;

    private static final String FIND_BY_FATURA_SQL = """
            SELECT id, fatura_id, lote_id, valor_total, status, retry_count,
                   trace_id, authorization_subject, motivo_recusa,
                   comprovante_confirmado, created_at
            FROM db_pagamentos.pagamento
            WHERE fatura_id = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcPagamentoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Pagamento save(Pagamento pagamento) {
        jdbcTemplate.update(
                UPSERT_SQL,
                pagamento.getId(),
                pagamento.getFaturaId(),
                pagamento.getLoteId(),
                pagamento.getValorTotal(),
                pagamento.getStatus().name(),
                pagamento.getRetryCount(),
                pagamento.getTraceId(),
                pagamento.getAuthorizationSubject(),
                pagamento.getMotivoRecusa(),
                pagamento.isComprovanteConfirmado());
        return pagamento;
    }

    @Override
    public Optional<Pagamento> findById(String id) {
        return jdbcTemplate.query(FIND_BY_ID_SQL, rs -> rs.next() ? Optional.of(map(rs)) : Optional.empty(), id);
    }

    @Override
    public Optional<Pagamento> findByFaturaId(String faturaId) {
        return jdbcTemplate.query(FIND_BY_FATURA_SQL, rs -> rs.next() ? Optional.of(map(rs)) : Optional.empty(), faturaId);
    }

    private Pagamento map(ResultSet rs) throws java.sql.SQLException {
        return new Pagamento(
                rs.getString("id"),
                rs.getString("fatura_id"),
                rs.getString("lote_id"),
                rs.getBigDecimal("valor_total"),
                rs.getString("trace_id"),
                rs.getString("authorization_subject"),
                PagamentoStatus.valueOf(rs.getString("status")),
                rs.getInt("retry_count"),
                rs.getString("motivo_recusa"),
                rs.getBoolean("comprovante_confirmado"),
                toInstant(rs.getTimestamp("created_at")));
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? Instant.now() : timestamp.toInstant();
    }
}
