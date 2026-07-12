package com.fabriciosanches.adamodulo3.comprovantes.adapter.out.persistence.redis;

import com.fabriciosanches.adamodulo3.comprovantes.application.port.out.ComprovanteCachePort;
import com.fabriciosanches.adamodulo3.comprovantes.domain.Comprovante;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RedisComprovanteCacheAdapter implements ComprovanteCachePort {

    private static final int TTL_SECONDS = 86400;

    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;

    public RedisComprovanteCacheAdapter(JedisPool jedisPool, ObjectMapper objectMapper) {
        this.jedisPool = jedisPool;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<Comprovante> get(String id) {
        try (Jedis jedis = jedisPool.getResource()) {
            String value = jedis.get(cacheKey(id));
            if (value == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(value, Comprovante.class));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to read comprovante cache", ex);
        }
    }

    @Override
    public void put(Comprovante comprovante) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.setex(cacheKey(comprovante.getId()), TTL_SECONDS, objectMapper.writeValueAsString(comprovante));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to write comprovante cache", ex);
        }
    }

    private String cacheKey(String id) {
        return "comprovantes:" + id;
    }
}
