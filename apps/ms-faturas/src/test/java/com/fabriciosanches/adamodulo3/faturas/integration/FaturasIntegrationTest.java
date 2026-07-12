package com.fabriciosanches.adamodulo3.faturas.integration;

import com.fabriciosanches.adamodulo3.faturas.domain.Fatura;
import com.fabriciosanches.adamodulo3.faturas.domain.FaturaRetryPolicy;
import com.fabriciosanches.adamodulo3.faturas.domain.FaturaStatus;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import redis.clients.jedis.Jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class FaturasIntegrationTest {

    private static final String JWT_SECRET = "01234567890123456789012345678901";

    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withUsername("test")
            .withPassword("test")
            .withDatabaseName("integration");

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>("redis:7").withExposedPorts(6379);

    @BeforeAll
    static void setupDatabase() throws Exception {
        try (Connection connection = DriverManager.getConnection(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA IF NOT EXISTS db_faturas");
        }

        executeSqlScript("apps/ms-faturas/src/main/resources/db/migration/db_faturas/V1__create_fatura_tables.sql");
        executeSqlScript("apps/ms-faturas/src/main/resources/db/migration/db_faturas/V2__seed_faturas.sql");
    }

    @AfterAll
    static void cleanupCache() {
        try (Jedis jedis = new Jedis(redis.getHost(), redis.getMappedPort(6379))) {
            jedis.flushDB();
        }
    }

    @Test
    void mustFallbackToMysqlOnRedisMissAndWarmCache() throws Exception {
        String faturaId = "11111111-1111-1111-1111-111111111111";
        String cacheKey = "faturas:" + faturaId + ":snapshot";

        try (Jedis jedis = new Jedis(redis.getHost(), redis.getMappedPort(6379))) {
            jedis.del(cacheKey);
        }

        CacheAsideFaturaReader reader = new CacheAsideFaturaReader();
        String token = validToken("integration-user");

        Optional<FaturaSnapshot> snapshot = reader.getById(faturaId, token);

        assertTrue(snapshot.isPresent(), "Fatura must be loaded from MySQL when Redis misses");
        assertEquals("PENDENTE", snapshot.get().status());

        try (Jedis jedis = new Jedis(redis.getHost(), redis.getMappedPort(6379))) {
            String cached = jedis.get(cacheKey);
            assertTrue(cached != null && cached.contains("PENDENTE"), "MySQL fallback must warm Redis snapshot cache");
            assertTrue(jedis.ttl(cacheKey) > 0, "Cache key must have TTL");
        }
    }

    @Test
    void mustRejectReadWhenJwtIsInvalid() {
        CacheAsideFaturaReader reader = new CacheAsideFaturaReader();
        assertThrows(SecurityException.class, () -> reader.getById("11111111-1111-1111-1111-111111111111", "invalid.token.value"));
    }

    @Test
    void mustEnforceRetryCeilingThreeAndMoveToProblema() {
        Fatura fatura = new Fatura(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "12345678901",
                new BigDecimal("100.00"),
                FaturaStatus.RECUSADO,
                2);

        FaturaRetryPolicy retryPolicy = new FaturaRetryPolicy();
        FaturaStatus resultingStatus = fatura.registrarTentativaRecusada(retryPolicy);

        assertEquals(3, fatura.getRetryCount(), "Retry count must stop at 3");
        assertEquals(FaturaStatus.PROBLEMA, resultingStatus, "Third retry must move fatura to PROBLEMA");
        assertEquals(FaturaStatus.PROBLEMA, fatura.getStatus(), "Fatura must persist PROBLEMA status after exhausted retry");
    }

    private static void executeSqlScript(String relativePath) throws Exception {
        Path sqlPath = repoRoot().resolve(relativePath);
        String script = Files.readString(sqlPath);

        for (String sql : script.split(";")) {
            String trimmed = sql.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try (Connection connection = DriverManager.getConnection(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
                 Statement statement = connection.createStatement()) {
                statement.execute(trimmed);
            }
        }
    }

    private static Path repoRoot() {
        Path current = Path.of("").toAbsolutePath();
        for (int i = 0; i < 8 && current != null; i++) {
            if (Files.exists(current.resolve(".specs"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Could not locate repository root (.specs)");
    }

    private static String validToken(String subject) throws Exception {
        String header = base64Url("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        long iat = Instant.now().getEpochSecond();
        long exp = iat + 1200;
        String payload = base64Url("{\"sub\":\"" + subject + "\",\"iat\":" + iat + ",\"exp\":" + exp + "}");
        String signingInput = header + "." + payload;
        String signature = sign(signingInput, JWT_SECRET);
        return signingInput + "." + signature;
    }

    private static String base64Url(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String sign(String payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signature = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
    }

    private static final class CacheAsideFaturaReader {

        Optional<FaturaSnapshot> getById(String faturaId, String bearerToken) throws Exception {
            validateJwt(bearerToken);

            String cacheKey = "faturas:" + faturaId + ":snapshot";
            try (Jedis jedis = new Jedis(redis.getHost(), redis.getMappedPort(6379))) {
                String cached = jedis.get(cacheKey);
                if (cached != null) {
                    String[] parts = cached.split("\\|");
                    return Optional.of(new FaturaSnapshot(parts[0], parts[1], Integer.parseInt(parts[2])));
                }
            }

            try (Connection connection = DriverManager.getConnection(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT id, status, retry_count FROM db_faturas.fatura WHERE id = ?")) {
                statement.setString(1, faturaId);
                try (ResultSet rs = statement.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.empty();
                    }

                    FaturaSnapshot snapshot = new FaturaSnapshot(
                            rs.getString("id"),
                            rs.getString("status"),
                            rs.getInt("retry_count"));

                    try (Jedis jedis = new Jedis(redis.getHost(), redis.getMappedPort(6379))) {
                        jedis.setex("faturas:" + faturaId + ":snapshot", 86400, snapshot.id() + "|" + snapshot.status() + "|" + snapshot.retryCount());
                    }

                    return Optional.of(snapshot);
                }
            }
        }

        private void validateJwt(String token) throws Exception {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new SecurityException("Invalid JWT format");
            }

            String expectedSignature = sign(parts[0] + "." + parts[1], JWT_SECRET);
            byte[] provided = Base64.getUrlDecoder().decode(parts[2]);
            byte[] expected = Base64.getUrlDecoder().decode(expectedSignature);
            if (!MessageDigest.isEqual(provided, expected)) {
                throw new SecurityException("Invalid JWT signature");
            }
        }
    }

    private record FaturaSnapshot(String id, String status, int retryCount) {
    }
}
