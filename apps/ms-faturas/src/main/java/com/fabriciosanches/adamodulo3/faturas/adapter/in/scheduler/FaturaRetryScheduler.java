package com.fabriciosanches.adamodulo3.faturas.adapter.in.scheduler;

import com.fabriciosanches.adamodulo3.faturas.application.model.GetFaturaResult;
import com.fabriciosanches.adamodulo3.faturas.application.port.out.FaturaCachePort;
import com.fabriciosanches.adamodulo3.faturas.application.port.out.ProblemaFaturaPublisherPort;
import com.fabriciosanches.adamodulo3.faturas.application.port.out.FaturaRepository;
import com.fabriciosanches.adamodulo3.faturas.application.port.out.PaymentRequestPublisher;
import com.fabriciosanches.adamodulo3.faturas.config.FaturasObservability;
import com.fabriciosanches.adamodulo3.faturas.domain.Fatura;
import com.fabriciosanches.adamodulo3.faturas.domain.FaturaRetryPolicy;
import com.fabriciosanches.adamodulo3.faturas.domain.FaturaStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.scheduling.annotation.Scheduled;

public class FaturaRetryScheduler {

    private final FaturaRepository faturaRepository;
    private final FaturaCachePort cachePort;
    private final PaymentRequestPublisher paymentRequestPublisher;
    private final ProblemaFaturaPublisherPort problemaFaturaPublisher;
    private final FaturaRetryPolicy retryPolicy;
    private final FaturasObservability observability;

    public FaturaRetryScheduler(
            FaturaRepository faturaRepository,
            FaturaCachePort cachePort,
            PaymentRequestPublisher paymentRequestPublisher,
            ProblemaFaturaPublisherPort problemaFaturaPublisher,
            FaturaRetryPolicy retryPolicy,
            FaturasObservability observability) {
        this.faturaRepository = faturaRepository;
        this.cachePort = cachePort;
        this.paymentRequestPublisher = paymentRequestPublisher;
        this.problemaFaturaPublisher = problemaFaturaPublisher;
        this.retryPolicy = retryPolicy;
        this.observability = observability;
    }

    @Scheduled(fixedDelayString = "${faturas.retry.fixed-delay-ms:120000}")
    public void reprocessarRecusadas() {
        List<Fatura> recusadas = faturaRepository.findByStatus(FaturaStatus.RECUSADO);
        for (Fatura fatura : recusadas) {
            String traceId = fatura.ensureTraceIdOrigem(UUID.randomUUID().toString());
            FaturaStatus nextStatus = fatura.registrarTentativaRecusada(retryPolicy);
            Fatura saved = faturaRepository.save(fatura);

            observability.onRetryAttempt(saved);

            if (nextStatus == FaturaStatus.SOLICITADO) {
                paymentRequestPublisher.publishPaymentRequested(
                        saved.getId(),
                        saved.getLoteId(),
                        saved.getValorTotal(),
                        traceId,
                        "retry-scheduler");
            } else if (nextStatus == FaturaStatus.PROBLEMA) {
                observability.onProblemaTransition(saved);
                String idempotencyKey = saved.getId() + ":retry-exhausted:" + saved.getRetryCount();
                problemaFaturaPublisher.publishProblemaFatura(
                        saved.getId(),
                        saved.getRetryCount(),
                        idempotencyKey,
                        traceId,
                        "Retry ceiling exhausted",
                        "{\"source\":\"fatura-retry-scheduler\"}");
            }

            GetFaturaResult snapshot = new GetFaturaResult(
                    saved.getId(),
                    saved.getLoteId(),
                    saved.getClienteDocumento(),
                    saved.getValorTotal(),
                    saved.getStatus(),
                    saved.getRetryCount());
            cachePort.putSnapshot(snapshot);
            cachePort.putStatus(saved.getId(), saved.getStatus(), saved.getRetryCount());
        }
    }
}
