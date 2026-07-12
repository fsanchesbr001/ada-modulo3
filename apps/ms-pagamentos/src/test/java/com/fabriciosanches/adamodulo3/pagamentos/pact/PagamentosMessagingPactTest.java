package com.fabriciosanches.adamodulo3.pagamentos.pact;

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
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PagamentosMessagingPactTest {

    @Test
    void messagingFlowMustForbidSyncPagoBeforeComprovanteConfirmation() {
        InMemoryPagamentoRepository repository = new InMemoryPagamentoRepository();
        ProcessPagamentoUseCase useCase = processUseCase(repository);

        Pagamento processed = useCase.processPagarEvent(new PagarEvent(
                "evt-1",
                "fatura-1",
                "lote-1",
                new BigDecimal("10.00"),
                Instant.now(),
                "trace-1",
                "admin"));

        assertNotEquals(PagamentoStatus.PAGO, processed.getStatus());
        assertEquals(PagamentoStatus.AGUARDANDO_COMPROVANTE, processed.getStatus());

        Optional<Pagamento> confirmed = useCase.finalizePagamentoIfConfirmed("fatura-1", "trace-1");
        assertTrue(confirmed.isPresent());
        assertEquals(PagamentoStatus.PAGO, confirmed.get().getStatus());
    }

    @Test
    void compensationPayloadMustKeepContractCompliantFields() {
        InMemoryPagamentoRepository repository = new InMemoryPagamentoRepository();
        ProcessPagamentoUseCase useCase = processUseCase(repository);

        Pagamento compensated = useCase.processPagarEvent(new PagarEvent(
                "evt-9",
                "fatura-9",
                "lote-9",
                new BigDecimal("22.00"),
                Instant.now(),
                "trace-9",
                "admin"));

        Map<String, Object> payload = Map.of(
                "fatura_id", compensated.getFaturaId(),
                "trace_id", compensated.getTraceId(),
                "status", compensated.getStatus().name(),
                "motivo", compensated.getMotivoRecusa());

        assertAll(
                () -> assertEquals("fatura-9", payload.get("fatura_id")),
                () -> assertEquals("trace-9", payload.get("trace_id")),
                () -> assertEquals("COMPENSADO", payload.get("status")),
                () -> assertTrue(String.valueOf(payload.get("motivo")).contains("0/4/9")));
    }

    @Test
    void messagingContractsMustDeclarePagarAndComprovanteChannels() throws IOException {
        String pagarYaml = Files.readString(repoRoot().resolve(Path.of(".specs", "asyncapi", "pagar-event.yaml"))).toLowerCase();
        String comprovanteYaml = Files.readString(repoRoot().resolve(Path.of(".specs", "asyncapi", "comprovante-gerado.yaml"))).toLowerCase();

        assertAll(
                () -> assertTrue(pagarYaml.contains("pagar"), "PAGAR channel must exist"),
                () -> assertTrue(pagarYaml.contains("trace_id"), "PAGAR contract must propagate trace_id"),
                () -> assertTrue(comprovanteYaml.contains("comprovante.gerado.topic"), "Comprovante confirmation topic must exist"),
                () -> assertTrue(comprovanteYaml.contains("payload_pdf_json"), "Comprovante payload contract must include payload_pdf_json"));
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

    private static Path repoRoot() {
        Path current = Path.of("").toAbsolutePath();
        for (int i = 0; i < 8 && current != null; i++) {
            if (Files.exists(current.resolve(".specs"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Could not locate repository root (.specs)");
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
