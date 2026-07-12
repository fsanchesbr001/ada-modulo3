package com.fabriciosanches.adamodulo3.pagamentos.unit;

import com.fabriciosanches.adamodulo3.pagamentos.adapter.in.web.PagamentosMockGatewayController;
import com.fabriciosanches.adamodulo3.pagamentos.adapter.in.web.dto.PagamentosMockGatewayRequest;
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
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PagamentosMockGatewayControllerTest {

    @Test
    void processarLoteMustReturnAcceptedWithFaturaIdAndStatus() {
        RecordingProcessPagamentoUseCase useCase = new RecordingProcessPagamentoUseCase();
        PagamentosMockGatewayController controller = new PagamentosMockGatewayController(useCase);

        PagamentosMockGatewayRequest request = new PagamentosMockGatewayRequest(
                "evt-1",
                "fatura-1",
                "lote-1",
                new BigDecimal("10.00"),
                "trace-1",
                "admin");

        ResponseEntity<?> response = controller.processarLote(request);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("fatura-1", ((com.fabriciosanches.adamodulo3.pagamentos.adapter.in.web.dto.PagamentosMockGatewayResponse) response.getBody()).faturaId());
        assertEquals(PagamentoStatus.AGUARDANDO_COMPROVANTE.name(), ((com.fabriciosanches.adamodulo3.pagamentos.adapter.in.web.dto.PagamentosMockGatewayResponse) response.getBody()).status());
        assertEquals("fatura-1", useCase.lastEvent.faturaId());
    }

    private static final class RecordingProcessPagamentoUseCase extends ProcessPagamentoUseCase {

        private PagarEvent lastEvent;

        private RecordingProcessPagamentoUseCase() {
            super(new NoopRepository(), null, new CompensatePagamentoUseCase(new NoopRepository(), observability()), new PagamentoFinalityPolicy(), observability());
        }

        @Override
        public Pagamento processPagarEvent(PagarEvent event) {
            this.lastEvent = event;
            return new Pagamento(
                    "pg-1",
                    event.faturaId(),
                    event.loteId(),
                    event.valorTotal(),
                    event.traceId(),
                    event.authorizationSubject(),
                    PagamentoStatus.AGUARDANDO_COMPROVANTE,
                    0,
                    null,
                    false,
                    Instant.now());
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
