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
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProblemaFaturaRoutingIntegrationTest {

    @Test
    void consumerFlowMustPersistPayloadPropagateTraceAndIncreaseMetric() {
        InMemoryProblemaRepository repository = new InMemoryProblemaRepository();
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        Counter routesCounter = Counter.builder("backoffice_problem_routes_total").register(meterRegistry);

        BackofficeObservability observability = new BackofficeObservability(routesCounter);
        RegisterProblemaFaturaUseCase useCase = new RegisterProblemaFaturaUseCase(repository, observability);
        ProblemaFaturaConsumer consumer = new ProblemaFaturaConsumer(useCase, new ObjectMapper());

        String payload = payload(
                "event-9",
                "fatura-9:retry-exhausted:3",
                "trace-9",
                "fatura-9",
                3,
                "Retry ceiling exhausted",
                "{\"source\":\"fatura-retry-scheduler\"}",
                "2026-01-01T10:00:00Z");

        consumer.consume(payload);

        assertEquals(1, repository.saved().size(), "Flow must persist one problema fatura intake record");
        ProblemaFaturaRecord saved = repository.saved().get(0);
        assertEquals("trace-9", saved.traceId(), "Flow must preserve trace_id from routing payload");
        assertEquals("{\"source\":\"fatura-retry-scheduler\"}", saved.payloadContexto(), "Flow must persist payload_contexto");
        assertEquals(1.0d, meterRegistry.find("backoffice_problem_routes_total").counter().count(), "Flow must increment backoffice routing metric when registered");
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

    private static final class InMemoryProblemaRepository extends ProblemaFaturaJdbcRepository {

        private final List<ProblemaFaturaRecord> saved = new ArrayList<>();

        private InMemoryProblemaRepository() {
            super(new JdbcTemplate());
        }

        @Override
        public boolean saveIfAbsent(ProblemaFaturaRecord record) {
            saved.add(record);
            return true;
        }

        private List<ProblemaFaturaRecord> saved() {
            return saved;
        }
    }
}
