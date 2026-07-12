package com.fabriciosanches.adamodulo3.pagamentos.integration;

import com.fabriciosanches.adamodulo3.pagamentos.adapter.out.gatewaymock.MockGatewayClient;
import com.fabriciosanches.adamodulo3.pagamentos.application.CompensatePagamentoUseCase;
import com.fabriciosanches.adamodulo3.pagamentos.application.ProcessPagamentoUseCase;
import com.fabriciosanches.adamodulo3.pagamentos.application.model.PagarEvent;
import com.fabriciosanches.adamodulo3.pagamentos.application.port.out.PagamentoRepository;
import com.fabriciosanches.adamodulo3.pagamentos.config.PagamentosObservability;
import com.fabriciosanches.adamodulo3.pagamentos.domain.Pagamento;
import com.fabriciosanches.adamodulo3.pagamentos.domain.PagamentoFinalityPolicy;
import com.fabriciosanches.adamodulo3.pagamentos.domain.PagamentoStatus;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PagamentosIntegrationTest {

    @Test
    void shouldHandleProcessandoRecusadoCompensadoAndAsyncPagoFinalization() {
        InMemoryPagamentoRepository repository = new InMemoryPagamentoRepository();
        ProcessPagamentoUseCase useCase = processUseCase(repository);

        Pagamento recusadoCompensado = useCase.processPagarEvent(new PagarEvent(
                "evt-0",
                "fatura-0",
                "lote-0",
                new BigDecimal("30.00"),
                Instant.now(),
                "trace-0",
                "admin"));

        assertEquals(PagamentoStatus.COMPENSADO, recusadoCompensado.getStatus());

        Pagamento aguardandoComprovante = useCase.processPagarEvent(new PagarEvent(
                "evt-1",
                "fatura-1",
                "lote-1",
                new BigDecimal("31.00"),
                Instant.now(),
                "trace-1",
                "admin"));

        assertEquals(PagamentoStatus.AGUARDANDO_COMPROVANTE, aguardandoComprovante.getStatus());

        Optional<Pagamento> finalizado = useCase.finalizePagamentoIfConfirmed("fatura-1", "trace-1");
        assertEquals(PagamentoStatus.PAGO, finalizado.orElseThrow().getStatus());
    }

    private static ProcessPagamentoUseCase processUseCase(InMemoryPagamentoRepository repository) {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        PagamentosObservability observability = new PagamentosObservability(
                registry.counter("pagamentos_pago_blocked_total"),
                registry.counter("pagamentos_compensations_total"),
                registry.counter("pagamentos_pagar_consumed_total"),
                registry.counter("pagamentos_pago_confirmed_total"));

        CompensatePagamentoUseCase compensateUseCase = new CompensatePagamentoUseCase(repository, observability);
        return new ProcessPagamentoUseCase(
                repository,
                new MockGatewayClient(),
                compensateUseCase,
                new PagamentoFinalityPolicy(),
                observability);
    }

    private static final class InMemoryPagamentoRepository implements PagamentoRepository {
        private final Map<String, Pagamento> byId = new HashMap<>();
        private final Map<String, Pagamento> byFatura = new HashMap<>();

        @Override
        public Pagamento save(Pagamento pagamento) {
            byId.put(pagamento.getId(), pagamento);
            byFatura.put(pagamento.getFaturaId(), pagamento);
            return pagamento;
        }

        @Override
        public Optional<Pagamento> findById(String id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public Optional<Pagamento> findByFaturaId(String faturaId) {
            return Optional.ofNullable(byFatura.get(faturaId));
        }
    }
}
