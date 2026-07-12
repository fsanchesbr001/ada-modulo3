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

class ProblemaFaturaRoutingIdempotencyTest {

    @Test
    void duplicateExhaustedLifecycleMustBeRegisteredExactlyOnce() {
        DedupRepository repository = new DedupRepository();
        BackofficeObservability observability = observability();
        RegisterProblemaFaturaUseCase useCase = new RegisterProblemaFaturaUseCase(repository, observability);
        ProblemaFaturaConsumer consumer = new ProblemaFaturaConsumer(useCase, new ObjectMapper());

        String payload = payload(
                "event-44",
                "fatura-44:retry-exhausted:3",
                "trace-44",
                "fatura-44",
                3,
                "Retry ceiling exhausted",
                "{\"source\":\"scheduler\"}",
                "2026-01-01T10:00:00Z");

        consumer.consume(payload);
        consumer.consume(payload);

        assertEquals(1, repository.persistedCount(), "A single exhausted lifecycle cannot create duplicate backoffice registrations");
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
            throw new IllegalStateException("Failed to build payload", ex);
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

    private static final class DedupRepository extends ProblemaFaturaJdbcRepository {

        private final Set<String> idempotencyKeys = new HashSet<>();

        private DedupRepository() {
            super(new JdbcTemplate());
        }

        @Override
        public boolean saveIfAbsent(ProblemaFaturaRecord record) {
            return idempotencyKeys.add(record.idempotencyKey());
        }

        private int persistedCount() {
            return idempotencyKeys.size();
        }
    }
}
