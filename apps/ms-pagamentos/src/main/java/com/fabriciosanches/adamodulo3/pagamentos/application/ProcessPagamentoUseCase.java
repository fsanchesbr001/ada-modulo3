package com.fabriciosanches.adamodulo3.pagamentos.application;

import com.fabriciosanches.adamodulo3.pagamentos.adapter.out.gatewaymock.MockGatewayClient;
import com.fabriciosanches.adamodulo3.pagamentos.application.model.PagarEvent;
import com.fabriciosanches.adamodulo3.pagamentos.application.port.out.PagamentoRepository;
import com.fabriciosanches.adamodulo3.pagamentos.config.PagamentosObservability;
import com.fabriciosanches.adamodulo3.pagamentos.domain.Pagamento;
import com.fabriciosanches.adamodulo3.pagamentos.domain.PagamentoFinalityPolicy;
import java.util.Optional;

public class ProcessPagamentoUseCase {

    private final PagamentoRepository repository;
    private final MockGatewayClient gatewayClient;
    private final CompensatePagamentoUseCase compensateUseCase;
    private final PagamentoFinalityPolicy finalityPolicy;
    private final PagamentosObservability observability;

    public ProcessPagamentoUseCase(
            PagamentoRepository repository,
            MockGatewayClient gatewayClient,
            CompensatePagamentoUseCase compensateUseCase,
            PagamentoFinalityPolicy finalityPolicy,
            PagamentosObservability observability) {
        this.repository = repository;
        this.gatewayClient = gatewayClient;
        this.compensateUseCase = compensateUseCase;
        this.finalityPolicy = finalityPolicy;
        this.observability = observability;
    }

    public Pagamento processPagarEvent(PagarEvent event) {
        observability.onPagarConsumed(event.faturaId(), event.traceId());

        Pagamento pagamento = repository.findByFaturaId(event.faturaId())
                .orElseGet(() -> Pagamento.createPending(
                        event.faturaId(),
                        event.loteId(),
                        event.valorTotal(),
                        event.traceId(),
                        event.authorizationSubject()));

        pagamento.markProcessando();

        if (!gatewayClient.shouldApprove(event.loteId())) {
            pagamento.markRecusado("Gateway mock refusal by lote suffix rule (0/4/9)");
            repository.save(pagamento);
            return compensateUseCase.compensate(pagamento);
        }

        pagamento.markAguardandoComprovante();
        return repository.save(pagamento);
    }

    public Optional<Pagamento> finalizePagamentoIfConfirmed(String faturaId, String traceId) {
        Optional<Pagamento> maybePagamento = repository.findByFaturaId(faturaId);
        if (maybePagamento.isEmpty()) {
            return Optional.empty();
        }

        Pagamento pagamento = maybePagamento.get();
        pagamento.confirmComprovante();

        if (!finalityPolicy.canFinalizeAsPago(pagamento)) {
            observability.onPagoBlocked(faturaId, traceId);
            repository.save(pagamento);
            return Optional.of(pagamento);
        }

        pagamento.markPago();
        Pagamento saved = repository.save(pagamento);
        observability.onPagoConfirmed(saved.getFaturaId(), traceId);
        return Optional.of(saved);
    }
}
