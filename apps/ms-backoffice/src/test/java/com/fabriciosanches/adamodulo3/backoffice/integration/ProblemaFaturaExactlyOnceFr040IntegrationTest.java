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

class ProblemaFaturaExactlyOnceFr040IntegrationTest {

    @Test
    void consumerMustRegisterExactlyOnceWhenReceivingDuplicateRoutingEvents() {
        InMemoryDedupProblemaRepository repository = new InMemoryDedupProblemaRepository();
        RegisterProblemaFaturaUseCase useCase = new RegisterProblemaFaturaUseCase(repository, observability());
        ProblemaFaturaConsumer consumer = new ProblemaFaturaConsumer(useCase, new ObjectMapper());

        String duplicatedPayload = payload(
                "event-1",
                "fatura-123:retry-exhausted:3",
                "trace-abc",
                "fatura-123",
                3,
                "Retry ceiling exhausted",
                "{\"source\":\"fatura-retry-scheduler\"}",
                "2026-01-01T10:00:00Z");

        consumer.consume(duplicatedPayload);
        consumer.consume(duplicatedPayload);

        assertEquals(1, repository.persistedCount(), "Duplicate messages must result in one persisted exhausted lifecycle intake");
    }

    private static BackofficeObservability observability() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        Counter routes = Counter.builder("backoffice_problem_routes_total").register(registry);
        return new BackofficeObservability(routes);
    }

    private static String payload(
            String eventId,
            String idempotencyKey,
            String traceId,
            String faturaId,
            int retryCountFinal,
            String motivo,
            String payloadContexto,
            String routedAt) {
        try {
            return new ObjectMapper().writeValueAsString(new RoutingPayload(
                    eventId,
                    idempotencyKey,
                    traceId,
                    faturaId,
                    retryCountFinal,
                    motivo,
                    payloadContexto,
                    routedAt));
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
