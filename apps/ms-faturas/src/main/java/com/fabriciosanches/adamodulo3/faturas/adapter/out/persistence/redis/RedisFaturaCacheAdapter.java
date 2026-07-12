package com.fabriciosanches.adamodulo3.faturas.adapter.out.persistence.redis;

import com.fabriciosanches.adamodulo3.faturas.application.model.GetFaturaResult;
import com.fabriciosanches.adamodulo3.faturas.application.port.out.FaturaCachePort;
import com.fabriciosanches.adamodulo3.faturas.domain.FaturaStatus;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RedisFaturaCacheAdapter implements FaturaCachePort {

    private static final int TTL_SECONDS = 60 * 60 * 24;

    private final JedisPool jedisPool;

    public RedisFaturaCacheAdapter(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    @Override
    public Optional<GetFaturaResult> getSnapshot(UUID faturaId) {
        String snapshotKey = snapshotKey(faturaId);
        try (Jedis jedis = jedisPool.getResource()) {
            String raw = jedis.get(snapshotKey);
            if (raw == null || raw.isBlank()) {
                return Optional.empty();
            }

            String[] parts = raw.split("\\|", -1);
            if (parts.length != 6) {
                return Optional.empty();
            }

            return Optional.of(new GetFaturaResult(
                    UUID.fromString(parts[0]),
                    UUID.fromString(parts[1]),
                    parts[2],
                    new BigDecimal(parts[3]),
                    FaturaStatus.valueOf(parts[4]),
                    Integer.parseInt(parts[5])));
        }
    }

    @Override
    public void putSnapshot(GetFaturaResult snapshot) {
        String payload = String.join("|",
                snapshot.id().toString(),
                snapshot.loteId().toString(),
                snapshot.clienteDocumento(),
                snapshot.valorTotal().toPlainString(),
                snapshot.status().name(),
                Integer.toString(snapshot.retryCount()));

        try (Jedis jedis = jedisPool.getResource()) {
            jedis.setex(snapshotKey(snapshot.id()), TTL_SECONDS, payload);
        }
    }

    @Override
    public void putStatus(UUID faturaId, FaturaStatus status, int retryCount) {
        String payload = status.name() + "|" + retryCount;
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.setex(statusKey(faturaId), TTL_SECONDS, payload);
        }
    }

    private String snapshotKey(UUID faturaId) {
        return "faturas:" + faturaId + ":snapshot";
    }

    private String statusKey(UUID faturaId) {
        return "faturas:" + faturaId + ":status";
    }
}
