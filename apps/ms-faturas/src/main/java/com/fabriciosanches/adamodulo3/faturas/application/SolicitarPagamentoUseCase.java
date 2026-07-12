package com.fabriciosanches.adamodulo3.faturas.application;

import com.fabriciosanches.adamodulo3.faturas.application.model.GetFaturaResult;
import com.fabriciosanches.adamodulo3.faturas.application.model.SolicitarPagamentoCommand;
import com.fabriciosanches.adamodulo3.faturas.application.port.out.FaturaCachePort;
import com.fabriciosanches.adamodulo3.faturas.application.port.out.FaturaRepository;
import com.fabriciosanches.adamodulo3.faturas.application.port.out.PaymentRequestPublisher;
import com.fabriciosanches.adamodulo3.faturas.domain.Fatura;

public class SolicitarPagamentoUseCase {

    private final FaturaRepository faturaRepository;
    private final FaturaCachePort cachePort;
    private final PaymentRequestPublisher paymentRequestPublisher;

    public SolicitarPagamentoUseCase(
            FaturaRepository faturaRepository,
            FaturaCachePort cachePort,
            PaymentRequestPublisher paymentRequestPublisher) {
        this.faturaRepository = faturaRepository;
        this.cachePort = cachePort;
        this.paymentRequestPublisher = paymentRequestPublisher;
    }

    public GetFaturaResult execute(SolicitarPagamentoCommand command) {
        Fatura fatura = faturaRepository.findById(command.faturaId())
                .orElseThrow(() -> new FaturaNotFoundException(command.faturaId()));

        fatura.solicitarPagamento();
        Fatura saved = faturaRepository.save(fatura);

        paymentRequestPublisher.publishPaymentRequested(
                saved.getId(),
                saved.getLoteId(),
                saved.getValorTotal(),
                command.traceId(),
                command.authorizationSubject());

        GetFaturaResult result = new GetFaturaResult(
                saved.getId(),
                saved.getLoteId(),
                saved.getClienteDocumento(),
                saved.getValorTotal(),
                saved.getStatus(),
                saved.getRetryCount());

        cachePort.putSnapshot(result);
        cachePort.putStatus(saved.getId(), saved.getStatus(), saved.getRetryCount());
        return result;
    }
}
