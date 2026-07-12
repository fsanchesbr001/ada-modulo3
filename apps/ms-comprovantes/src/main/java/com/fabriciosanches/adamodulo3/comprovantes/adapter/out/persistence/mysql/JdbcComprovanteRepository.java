package com.fabriciosanches.adamodulo3.comprovantes.adapter.out.persistence.mysql;

import com.fabriciosanches.adamodulo3.comprovantes.application.port.out.ComprovanteRepository;
import com.fabriciosanches.adamodulo3.comprovantes.domain.Comprovante;
import com.fabriciosanches.adamodulo3.comprovantes.domain.ComprovanteStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;

public class JdbcComprovanteRepository implements ComprovanteRepository {

    private static final String INSERT_SQL = """
            INSERT INTO db_comprovantes.comprovante
                (id, payload_pdf_json, payload_notificacao_completo, status)
            VALUES (?, CAST(? AS JSON), CAST(? AS JSON), ?)
            ON DUPLICATE KEY UPDATE
                payload_pdf_json = VALUES(payload_pdf_json),
                payload_notificacao_completo = VALUES(payload_notificacao_completo),
                status = VALUES(status)
            """;

    private static final String FIND_BY_ID_SQL = """
            SELECT id, payload_pdf_json, payload_notificacao_completo, status, created_at
            FROM db_comprovantes.comprovante
            WHERE id = ?
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcComprovanteRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Comprovante save(Comprovante comprovante) {
        jdbcTemplate.update(
                INSERT_SQL,
                comprovante.getId(),
                asJson(comprovante.getPayloadPdfJson()),
                asJson(comprovante.getPayloadNotificacaoCompleto()),
                comprovante.getStatus().name());
        return comprovante;
    }

    @Override
    public Optional<Comprovante> findById(String id) {
        return jdbcTemplate.query(FIND_BY_ID_SQL, rs -> {
            if (!rs.next()) {
                return Optional.empty();
            }
            return Optional.of(new Comprovante(
                    rs.getString("id"),
                    readMap(rs.getString("payload_pdf_json")),
                    readOptionalMap(rs.getString("payload_notificacao_completo")),
                    ComprovanteStatus.valueOf(rs.getString("status")),
                    toInstant(rs.getTimestamp("created_at"))));
        }, id);
    }

    private String asJson(Map<String, Object> value) {
        if (value == null) {
            return "null";
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize comprovante payload", ex);
        }
    }

    private Map<String, Object> readMap(String value) {
        try {
            return objectMapper.readValue(value, new TypeReference<>() {
            });
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to deserialize comprovante payload", ex);
        }
    }

    private Map<String, Object> readOptionalMap(String value) {
        if (value == null || value.isBlank() || "null".equalsIgnoreCase(value)) {
            return null;
        }
        return readMap(value);
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? Instant.now() : timestamp.toInstant();
    }
}
