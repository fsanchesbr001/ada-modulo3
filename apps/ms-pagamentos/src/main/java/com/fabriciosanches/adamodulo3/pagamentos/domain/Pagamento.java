package com.fabriciosanches.adamodulo3.pagamentos.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Pagamento {

    private final String id;
    private final String faturaId;
    private final String loteId;
    private final BigDecimal valorTotal;
    private String traceId;
    private String authorizationSubject;
    private PagamentoStatus status;
    private int retryCount;
    private String motivoRecusa;
    private boolean comprovanteConfirmado;
    private final Instant createdAt;

    public Pagamento(
            String id,
            String faturaId,
            String loteId,
            BigDecimal valorTotal,
            String traceId,
            String authorizationSubject,
            PagamentoStatus status,
            int retryCount,
            String motivoRecusa,
            boolean comprovanteConfirmado,
            Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.faturaId = Objects.requireNonNull(faturaId, "faturaId is required");
        this.loteId = Objects.requireNonNull(loteId, "loteId is required");
        this.valorTotal = Objects.requireNonNull(valorTotal, "valorTotal is required");
        this.traceId = traceId;
        this.authorizationSubject = authorizationSubject;
        this.status = Objects.requireNonNull(status, "status is required");
        this.retryCount = retryCount;
        this.motivoRecusa = motivoRecusa;
        this.comprovanteConfirmado = comprovanteConfirmado;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
    }

    public static Pagamento createPending(
            String faturaId,
            String loteId,
            BigDecimal valorTotal,
            String traceId,
            String authorizationSubject) {
        return new Pagamento(
                UUID.randomUUID().toString(),
                faturaId,
                loteId,
                valorTotal,
                traceId,
                authorizationSubject,
                PagamentoStatus.PENDENTE,
                0,
                null,
                false,
                Instant.now());
    }

    public void markProcessando() {
        this.status = PagamentoStatus.PROCESSANDO;
    }

    public void markRecusado(String motivoRecusa) {
        this.status = PagamentoStatus.RECUSADO;
        this.retryCount++;
        this.motivoRecusa = motivoRecusa;
    }

    public void markCompensado() {
        this.status = PagamentoStatus.COMPENSADO;
    }

    public void markAguardandoComprovante() {
        this.status = PagamentoStatus.AGUARDANDO_COMPROVANTE;
    }

    public void confirmComprovante() {
        this.comprovanteConfirmado = true;
    }

    public void markPago() {
        this.status = PagamentoStatus.PAGO;
    }

    public String getId() {
        return id;
    }

    public String getFaturaId() {
        return faturaId;
    }

    public String getLoteId() {
        return loteId;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getAuthorizationSubject() {
        return authorizationSubject;
    }

    public PagamentoStatus getStatus() {
        return status;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public String getMotivoRecusa() {
        return motivoRecusa;
    }

    public boolean isComprovanteConfirmado() {
        return comprovanteConfirmado;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
