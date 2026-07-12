package com.fabriciosanches.adamodulo3.faturas.adapter.out.persistence.mysql;

import com.fabriciosanches.adamodulo3.faturas.application.port.out.FaturaRepository;
import com.fabriciosanches.adamodulo3.faturas.domain.Fatura;
import com.fabriciosanches.adamodulo3.faturas.domain.FaturaStatus;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;

public class JdbcFaturaRepository implements FaturaRepository {

    private static final String UPSERT_SQL = """
            INSERT INTO db_faturas.fatura (id, lote_id, cliente_documento, valor_total, status, retry_count)
            VALUES (?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                lote_id = VALUES(lote_id),
                cliente_documento = VALUES(cliente_documento),
                valor_total = VALUES(valor_total),
                status = VALUES(status),
                retry_count = VALUES(retry_count)
            """;

    private static final String FIND_BY_ID_SQL = """
            SELECT id, lote_id, cliente_documento, valor_total, status, retry_count
            FROM db_faturas.fatura
            WHERE id = ?
            """;

        private static final String FIND_BY_STATUS_SQL = """
            SELECT id, lote_id, cliente_documento, valor_total, status, retry_count
            FROM db_faturas.fatura
            WHERE status = ?
            """;

    private final DataSource dataSource;

    public JdbcFaturaRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Fatura save(Fatura fatura) {
        try (var connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPSERT_SQL)) {
            bind(statement, fatura);
            statement.executeUpdate();
            return fatura;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to persist fatura " + fatura.getId(), ex);
        }
    }

    @Override
    public List<Fatura> saveAll(List<Fatura> faturas) {
        List<Fatura> persisted = new ArrayList<>();
        for (Fatura fatura : faturas) {
            persisted.add(save(fatura));
        }
        return persisted;
    }

    @Override
    public Optional<Fatura> findById(UUID faturaId) {
        try (var connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {
            statement.setString(1, faturaId.toString());
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(map(rs));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to query fatura " + faturaId, ex);
        }
    }

    @Override
    public List<Fatura> findByStatus(FaturaStatus status) {
        try (var connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_STATUS_SQL)) {
            statement.setString(1, status.name());
            try (ResultSet rs = statement.executeQuery()) {
                List<Fatura> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(map(rs));
                }
                return result;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to query faturas by status " + status, ex);
        }
    }

    private void bind(PreparedStatement statement, Fatura fatura) throws SQLException {
        statement.setString(1, fatura.getId().toString());
        statement.setString(2, fatura.getLoteId().toString());
        statement.setString(3, fatura.getClienteDocumento());
        statement.setBigDecimal(4, fatura.getValorTotal());
        statement.setString(5, fatura.getStatus().name());
        statement.setInt(6, fatura.getRetryCount());
    }

    private Fatura map(ResultSet rs) throws SQLException {
        return new Fatura(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("lote_id")),
                rs.getString("cliente_documento"),
                rs.getBigDecimal("valor_total"),
                FaturaStatus.valueOf(rs.getString("status")),
                rs.getInt("retry_count"));
    }
}
