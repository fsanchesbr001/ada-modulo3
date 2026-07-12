package com.fabriciosanches.adamodulo3.pagamentos.unit;

import com.fabriciosanches.adamodulo3.pagamentos.adapter.in.messaging.ComprovanteGeradoConsumer;
import com.fabriciosanches.adamodulo3.pagamentos.application.CompensatePagamentoUseCase;
import com.fabriciosanches.adamodulo3.pagamentos.application.ProcessPagamentoUseCase;
import com.fabriciosanches.adamodulo3.pagamentos.application.model.PagarEvent;
import com.fabriciosanches.adamodulo3.pagamentos.application.port.out.PagamentoRepository;
import com.fabriciosanches.adamodulo3.pagamentos.config.PagamentosObservability;
import com.fabriciosanches.adamodulo3.pagamentos.domain.Pagamento;
import com.fabriciosanches.adamodulo3.pagamentos.domain.PagamentoFinalityPolicy;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ComprovanteGeradoConsumerTest {

    @Test
    void consumeMustUsePayloadFaturaIdWhenHeaderMissing() {
        RecordingUseCase useCase = new RecordingUseCase();
        ComprovanteGeradoConsumer consumer = new ComprovanteGeradoConsumer(useCase);

        consumer.consume(Map.of("fatura_id", "fatura-1"), "trace-1", "", "admin");

        assertEquals("fatura-1", useCase.lastFaturaId);
        assertEquals("trace-1", useCase.lastTraceId);
    }

    private static final class RecordingUseCase extends ProcessPagamentoUseCase {

        private String lastFaturaId;
        private String lastTraceId;

        private RecordingUseCase() {
            super(new NoopRepository(), null, new CompensatePagamentoUseCase(new NoopRepository(), observability()), new PagamentoFinalityPolicy(), observability());
        }

        @Override
        public Optional<Pagamento> finalizePagamentoIfConfirmed(String faturaId, String traceId) {
            this.lastFaturaId = faturaId;
            this.lastTraceId = traceId;
            return Optional.empty();
        }

        @Override
        public Pagamento processPagarEvent(PagarEvent event) {
            throw new UnsupportedOperationException();
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
