package com.fabriciosanches.adamodulo3.faturas.integration;

import com.fabriciosanches.adamodulo3.faturas.adapter.in.scheduler.FaturaRetryScheduler;
import com.fabriciosanches.adamodulo3.faturas.application.model.GetFaturaResult;
import com.fabriciosanches.adamodulo3.faturas.application.port.out.FaturaCachePort;
import com.fabriciosanches.adamodulo3.faturas.application.port.out.FaturaRepository;
import com.fabriciosanches.adamodulo3.faturas.application.port.out.PaymentRequestPublisher;
import com.fabriciosanches.adamodulo3.faturas.application.port.out.ProblemaFaturaPublisherPort;
import com.fabriciosanches.adamodulo3.faturas.config.FaturasObservability;
import com.fabriciosanches.adamodulo3.faturas.domain.Fatura;
import com.fabriciosanches.adamodulo3.faturas.domain.FaturaRetryPolicy;
import com.fabriciosanches.adamodulo3.faturas.domain.FaturaStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProblemaFaturaExactlyOnceFr040IntegrationTest {

    @Test
    void schedulerMustEmitExactlyOneProblemaRoutingPerExhaustedLifecycle() {
        Fatura exhaustedCandidate = new Fatura(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "12345678901",
                new BigDecimal("150.00"),
                UUID.randomUUID().toString(),
                FaturaStatus.RECUSADO,
                2);

        InMemoryFaturaRepository repository = new InMemoryFaturaRepository(exhaustedCandidate);
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
        scheduler.reprocessarRecusadas();

        assertEquals(FaturaStatus.PROBLEMA, exhaustedCandidate.getStatus());
        assertEquals(3, exhaustedCandidate.getRetryCount());
        assertEquals(1, problemaPublisher.publishedEvents().size(), "A single exhausted lifecycle must emit one backoffice routing event");
        assertEquals(
                exhaustedCandidate.getId() + ":retry-exhausted:3",
                problemaPublisher.publishedEvents().getFirst().idempotencyKey(),
                "Idempotency key must stay deterministic for FR-040 exact-once routing");
    }

    private static FaturasObservability observability() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        Counter cacheMiss = Counter.builder("faturas_cache_misses_total").register(registry);
        Counter retry = Counter.builder("faturas_retry_attempts_total").register(registry);
        Counter problema = Counter.builder("faturas_problema_transitions_total").register(registry);
        return new FaturasObservability(cacheMiss, retry, problema);
    }

    private record PublishedProblema(String idempotencyKey, String traceId) {
    }

    private static final class CapturingProblemaPublisher implements ProblemaFaturaPublisherPort {

        private final List<PublishedProblema> published = new ArrayList<>();

        @Override
        public void publishProblemaFatura(
                UUID faturaId,
                int retryCountFinal,
                String idempotencyKey,
                String traceId,
                String motivo,
                String payloadContexto) {
            published.add(new PublishedProblema(idempotencyKey, traceId));
        }

        private List<PublishedProblema> publishedEvents() {
            return published;
        }
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
}
