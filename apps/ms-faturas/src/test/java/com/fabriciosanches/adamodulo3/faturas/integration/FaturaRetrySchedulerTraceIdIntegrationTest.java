package com.fabriciosanches.adamodulo3.faturas.integration;

import com.fabriciosanches.adamodulo3.faturas.adapter.in.scheduler.FaturaRetryScheduler;
import com.fabriciosanches.adamodulo3.faturas.adapter.out.messaging.ProblemaFaturaPublisher;
import com.fabriciosanches.adamodulo3.faturas.application.model.GetFaturaResult;
import com.fabriciosanches.adamodulo3.faturas.application.port.out.FaturaCachePort;
import com.fabriciosanches.adamodulo3.faturas.application.port.out.FaturaRepository;
import com.fabriciosanches.adamodulo3.faturas.application.port.out.PaymentRequestPublisher;
import com.fabriciosanches.adamodulo3.faturas.application.port.out.ProblemaFaturaPublisherPort;
import com.fabriciosanches.adamodulo3.faturas.config.FaturasObservability;
import com.fabriciosanches.adamodulo3.faturas.domain.Fatura;
import com.fabriciosanches.adamodulo3.faturas.domain.FaturaRetryPolicy;
import com.fabriciosanches.adamodulo3.faturas.domain.FaturaStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class FaturaRetrySchedulerTraceIdIntegrationTest {

    @Test
    void schedulerMustCreateTraceIdWhenMissingAndPropagateToProblemaRoutingFlow() {
        Fatura fatura = new Fatura(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "12345678901",
                new BigDecimal("100.00"),
                null,
                FaturaStatus.RECUSADO,
                2);

        InMemoryFaturaRepository repository = new InMemoryFaturaRepository(fatura);
        CapturingProblemaPublisher problemaPublisher = new CapturingProblemaPublisher();

        FaturaRetryScheduler scheduler = new FaturaRetryScheduler(
                repository,
                new NoopCachePort(),
                (faturaId, loteId, valorTotal, traceId, subject) -> {
                },
                problemaPublisher,
                new FaturaRetryPolicy(),
                observability());

        scheduler.reprocessarRecusadas();

        assertEquals(FaturaStatus.PROBLEMA, fatura.getStatus(), "Scheduler must move fatura to PROBLEMA on exhausted retry");
        assertEquals(3, fatura.getRetryCount(), "Scheduler must stop at retry ceiling 3");
        assertNotNull(fatura.getTraceIdOrigem(), "Scheduler must create trace_id when missing");
        assertEquals(fatura.getTraceIdOrigem(), problemaPublisher.traceId(), "Generated trace_id must be propagated to problema routing publisher");
        assertEquals(fatura.getId() + ":retry-exhausted:3", problemaPublisher.idempotencyKey(), "Idempotency key must be deterministic for exhausted lifecycle");
    }

    @Test
    void problemaPublisherMustPropagateTraceIdToKafkaHeaders() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        ObjectMapper objectMapper = new ObjectMapper();
        ProblemaFaturaPublisher publisher = new ProblemaFaturaPublisher(kafkaTemplate, objectMapper, "problema-fatura-routing");

        UUID faturaId = UUID.randomUUID();
        String traceId = UUID.randomUUID().toString();

        publisher.publishProblemaFatura(
                faturaId,
                3,
                faturaId + ":retry-exhausted:3",
                traceId,
                "Retry ceiling exhausted",
                "{\"source\":\"test\"}");

        ArgumentCaptor<ProducerRecord<String, String>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(captor.capture());
        ProducerRecord<String, String> record = captor.getValue();

        assertEquals("problema-fatura-routing", record.topic());
        assertEquals(traceId, new String(record.headers().lastHeader("trace_id").value()));
        assertTrue(record.headers().lastHeader("idempotency_key") != null, "idempotency_key header must be present");
    }

    private static FaturasObservability observability() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        Counter cacheMiss = Counter.builder("faturas_cache_misses_total").register(registry);
        Counter retry = Counter.builder("faturas_retry_attempts_total").register(registry);
        Counter problema = Counter.builder("faturas_problema_transitions_total").register(registry);
        return new FaturasObservability(cacheMiss, retry, problema);
    }

    private static final class InMemoryFaturaRepository implements FaturaRepository {

        private final Fatura fatura;

        private InMemoryFaturaRepository(Fatura fatura) {
            this.fatura = fatura;
        }

        @Override
        public Fatura save(Fatura fatura) {
            return fatura;
        }

        @Override
        public List<Fatura> saveAll(List<Fatura> faturas) {
            return faturas;
        }

        @Override
        public Optional<Fatura> findById(UUID faturaId) {
            return Optional.of(fatura);
        }

        @Override
        public List<Fatura> findByStatus(FaturaStatus status) {
            if (status == fatura.getStatus()) {
                return List.of(fatura);
            }
            return List.of();
        }
    }

    private static final class NoopCachePort implements FaturaCachePort {

        @Override
        public Optional<GetFaturaResult> getSnapshot(UUID faturaId) {
            return Optional.empty();
        }

        @Override
        public void putSnapshot(GetFaturaResult snapshot) {
        }

        @Override
        public void putStatus(UUID faturaId, FaturaStatus status, int retryCount) {
        }
    }

    private static final class CapturingProblemaPublisher implements ProblemaFaturaPublisherPort {

        private String traceId;
        private String idempotencyKey;

        @Override
        public void publishProblemaFatura(
                UUID faturaId,
                int retryCountFinal,
                String idempotencyKey,
                String traceId,
                String motivo,
                String payloadContexto) {
            this.traceId = traceId;
            this.idempotencyKey = idempotencyKey;
        }

        private String traceId() {
            return traceId;
        }

        private String idempotencyKey() {
            return idempotencyKey;
        }
    }
}
