package com.fabriciosanches.adamodulo3.comprovantes.unit;

import com.fabriciosanches.adamodulo3.comprovantes.application.GetComprovanteUseCase;
import com.fabriciosanches.adamodulo3.comprovantes.application.port.out.ComprovanteCachePort;
import com.fabriciosanches.adamodulo3.comprovantes.application.port.out.ComprovanteRepository;
import com.fabriciosanches.adamodulo3.comprovantes.config.ComprovantesObservability;
import com.fabriciosanches.adamodulo3.comprovantes.domain.Comprovante;
import com.fabriciosanches.adamodulo3.comprovantes.domain.ComprovanteLookupPolicy;
import com.fabriciosanches.adamodulo3.comprovantes.domain.ComprovanteStatus;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GetComprovanteUseCaseTest {

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    @Test
    void shouldReturnFromCacheAndRecordCacheHit() {
        FakeRepository repository = new FakeRepository(Optional.empty());
        FakeCache cache = new FakeCache(Optional.of(sampleComprovante("c1")));
        ComprovantesObservability observability = observability();
        GetComprovanteUseCase useCase = new GetComprovanteUseCase(
                repository,
                cache,
                new ComprovanteLookupPolicy(),
                observability);

        Comprovante result = useCase.execute("c1");

        assertEquals("c1", result.getId());
        assertEquals(1.0, metricValue("comprovantes_cache_hits_total"));
        assertEquals(0.0, metricValue("comprovantes_cache_misses_total"));
        assertEquals(0, repository.calls);
    }

    @Test
    void shouldRetryAfterCacheMissUntilRepositoryFindsValue() {
        FakeRepository repository = new FakeRepository(Optional.empty(), Optional.of(sampleComprovante("c2")));
        FakeCache cache = new FakeCache(Optional.empty());
        ComprovantesObservability observability = observability();
        GetComprovanteUseCase useCase = new GetComprovanteUseCase(
                repository,
                cache,
                new ComprovanteLookupPolicy(),
                observability);

        Comprovante result = useCase.execute("c2");

        assertEquals("c2", result.getId());
        assertEquals(1.0, metricValue("comprovantes_cache_misses_total"));
        assertEquals(1.0, metricValue("comprovantes_get_retries_total"));
        assertEquals(2, repository.calls);
    }

    private ComprovantesObservability observability() {
        return new ComprovantesObservability(
                meterRegistry.counter("comprovantes_posts_accepted_total"),
                meterRegistry.counter("comprovantes_queue_publications_total"),
                meterRegistry.counter("comprovantes_consumer_failures_total"),
                meterRegistry.counter("comprovantes_cache_hits_total"),
                meterRegistry.counter("comprovantes_cache_misses_total"),
                meterRegistry.counter("comprovantes_get_retries_total"));
    }

    private double metricValue(String name) {
        return meterRegistry.counter(name).count();
    }

    private static Comprovante sampleComprovante(String id) {
        return new Comprovante(
                id,
                Map.of("k", "v"),
                null,
                ComprovanteStatus.ACEITO,
                Instant.now());
    }

    private static final class FakeRepository implements ComprovanteRepository {
        private final Optional<Comprovante>[] responses;
        private int index;
        private int calls;

        @SafeVarargs
        private FakeRepository(Optional<Comprovante>... responses) {
            this.responses = responses;
        }

        @Override
        public Comprovante save(Comprovante comprovante) {
            return comprovante;
        }

        @Override
        public Optional<Comprovante> findById(String id) {
            calls++;
            Optional<Comprovante> result = responses[Math.min(index, responses.length - 1)];
            index++;
            return result;
        }
    }

    private static final class FakeCache implements ComprovanteCachePort {
        private Optional<Comprovante> current;

        private FakeCache(Optional<Comprovante> current) {
            this.current = current;
        }

        @Override
        public Optional<Comprovante> get(String id) {
            return current;
        }

        @Override
        public void put(Comprovante comprovante) {
            this.current = Optional.of(comprovante);
        }
    }
}
