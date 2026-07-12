package com.fabriciosanches.adamodulo3.pagamentos.application;

import com.fabriciosanches.adamodulo3.pagamentos.application.port.out.PagamentoRepository;
import com.fabriciosanches.adamodulo3.pagamentos.config.PagamentosObservability;
import com.fabriciosanches.adamodulo3.pagamentos.domain.Pagamento;

public class CompensatePagamentoUseCase {

    private final PagamentoRepository repository;
    private final PagamentosObservability observability;

    public CompensatePagamentoUseCase(PagamentoRepository repository, PagamentosObservability observability) {
        this.repository = repository;
        this.observability = observability;
    }

    public Pagamento compensate(Pagamento pagamento) {
        pagamento.markCompensado();
        Pagamento saved = repository.save(pagamento);
        observability.onCompensation(saved.getFaturaId(), saved.getTraceId());
        return saved;
    }
}
