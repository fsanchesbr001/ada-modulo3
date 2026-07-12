package com.fabriciosanches.adamodulo3.backoffice.integration;

import com.fabriciosanches.adamodulo3.backoffice.adapter.in.messaging.ProblemaFaturaConsumer;
import com.fabriciosanches.adamodulo3.backoffice.adapter.out.persistence.mysql.ProblemaFaturaJdbcRepository;
import com.fabriciosanches.adamodulo3.backoffice.adapter.out.persistence.mysql.ProblemaFaturaRecord;
import com.fabriciosanches.adamodulo3.backoffice.application.RegisterProblemaFaturaUseCase;
import com.fabriciosanches.adamodulo3.backoffice.config.BackofficeObservability;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProblemaFaturaRoutingSc009QualityGateTest {

    @Test
    void mustPersistExactlyOneRoutingForSingleExhaustedLifecycleEvent() {
        InMemoryDedupProblemaRepository repository = new InMemoryDedupProblemaRepository();
        ProblemaFaturaConsumer consumer = buildConsumer(repository);

        consumer.consume(payload("event-a", "fatura-a:retry-exhausted:3", "fatura-a"));

        assertEquals(1, repository.persistedCount(), "SC-009: single exhausted lifecycle must produce exactly one route");
    }

    @Test
    void mustRemainExactlyOneRoutingWhenDuplicateEventsArrive() {
        InMemoryDedupProblemaRepository repository = new InMemoryDedupProblemaRepository();
        ProblemaFaturaConsumer consumer = buildConsumer(repository);

        String duplicatedPayload = payload("event-b", "fatura-b:retry-exhausted:3", "fatura-b");
        consumer.consume(duplicatedPayload);
        consumer.consume(duplicatedPayload);

        assertEquals(1, repository.persistedCount(), "SC-009: duplicate exhausted lifecycle events must still route exactly once");
    }

    private static ProblemaFaturaConsumer buildConsumer(InMemoryDedupProblemaRepository repository) {
        RegisterProblemaFaturaUseCase useCase = new RegisterProblemaFaturaUseCase(repository, observability());
        return new ProblemaFaturaConsumer(useCase, new ObjectMapper());
    }

    private static BackofficeObservability observability() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        Counter routes = Counter.builder("backoffice_problem_routes_total").register(registry);
        return new BackofficeObservability(routes);
    }

    private static String payload(String eventId, String idempotencyKey, String faturaId) {
        try {
            return new ObjectMapper().writeValueAsString(new RoutingPayload(
                    eventId,
                    idempotencyKey,
                    "trace-" + faturaId,
                    faturaId,
                    3,
                    "Retry ceiling exhausted",
                    "{\"source\":\"fatura-retry-scheduler\"}",
                    "2026-01-01T10:00:00Z"));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to build routing payload", ex);
        }
    }

    private record RoutingPayload(
            String eventId,
            String idempotencyKey,
            String traceId,
            String faturaId,
            int retryCountFinal,
            String motivo,
            String payloadContexto,
            String routedAt) {
    }

    private static final class InMemoryDedupProblemaRepository extends ProblemaFaturaJdbcRepository {

        private final Set<String> persistedIdempotencyKeys = new HashSet<>();

        private InMemoryDedupProblemaRepository() {
            super(new JdbcTemplate());
        }

        @Override
        public boolean saveIfAbsent(ProblemaFaturaRecord record) {
            return persistedIdempotencyKeys.add(record.idempotencyKey());
        }

        private int persistedCount() {
            return persistedIdempotencyKeys.size();
        }
    }
}
