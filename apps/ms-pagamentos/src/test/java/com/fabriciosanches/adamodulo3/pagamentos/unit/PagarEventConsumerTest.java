package com.fabriciosanches.adamodulo3.pagamentos.unit;

import com.fabriciosanches.adamodulo3.pagamentos.adapter.in.messaging.PagarEventConsumer;
import com.fabriciosanches.adamodulo3.pagamentos.application.CompensatePagamentoUseCase;
import com.fabriciosanches.adamodulo3.pagamentos.application.ProcessPagamentoUseCase;
import com.fabriciosanches.adamodulo3.pagamentos.application.model.PagarEvent;
import com.fabriciosanches.adamodulo3.pagamentos.application.port.out.PagamentoRepository;
import com.fabriciosanches.adamodulo3.pagamentos.config.PagamentosObservability;
import com.fabriciosanches.adamodulo3.pagamentos.domain.Pagamento;
import com.fabriciosanches.adamodulo3.pagamentos.domain.PagamentoFinalityPolicy;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PagarEventConsumerTest {

    @Test
    void consumeMustResolveAuthorizationFromBearerWhenSubjectHeaderMissing() {
        RecordingUseCase useCase = new RecordingUseCase();
        PagarEventConsumer consumer = new PagarEventConsumer(useCase);

        consumer.consume(
                Map.of(
                        "event_id", "evt-1",
                        "fatura_id", "fatura-1",
                        "lote_id", "lote-1",
                        "valor_total", "10.00",
                        "requested_at", "2026-01-01T00:00:00Z",
                        "trace_id", "trace-payload",
                        "authorization_subject", "subject-payload"),
                "trace-header",
                "",
                "Bearer abc");

        assertEquals("fatura-1", useCase.lastEvent.faturaId());
        assertEquals("trace-header", useCase.lastEvent.traceId());
        assertEquals("Bearer abc", useCase.lastEvent.authorizationSubject());
    }

    private static final class RecordingUseCase extends ProcessPagamentoUseCase {

        private PagarEvent lastEvent;

        private RecordingUseCase() {
            super(new NoopRepository(), null, new CompensatePagamentoUseCase(new NoopRepository(), observability()), new PagamentoFinalityPolicy(), observability());
        }

        @Override
        public Pagamento processPagarEvent(PagarEvent event) {
            this.lastEvent = event;
            return Pagamento.createPending(event.faturaId(), event.loteId(), BigDecimal.TEN, event.traceId(), event.authorizationSubject());
        }

        private static PagamentosObservability observability() {
            SimpleMeterRegistry registry = new SimpleMeterRegistry();
            return new PagamentosObservability(
                    registry.counter("pagamentos_pago_blocked_total"),
                    registry.counter("pagamentos_compensations_total"),
                    registry.counter("pagamentos_pagar_consumed_total"),
                    registry.counter("pagamentos_pago_confirmed_total"));
        }
    }

    private static final class NoopRepository implements PagamentoRepository {
        @Override
        public Pagamento save(Pagamento pagamento) {
            return pagamento;
        }

        @Override
        public Optional<Pagamento> findById(String id) {
            return Optional.empty();
        }

        @Override
        public Optional<Pagamento> findByFaturaId(String faturaId) {
            return Optional.empty();
        }
    }
}
