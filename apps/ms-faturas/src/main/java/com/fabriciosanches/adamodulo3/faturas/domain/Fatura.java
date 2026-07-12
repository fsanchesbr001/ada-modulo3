package com.fabriciosanches.adamodulo3.faturas.domain;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public class Fatura {

    private final UUID id;
    private final UUID loteId;
    private final String clienteDocumento;
    private final BigDecimal valorTotal;
    private FaturaStatus status;
    private int retryCount;

    public Fatura(UUID id, UUID loteId, String clienteDocumento, BigDecimal valorTotal, FaturaStatus status, int retryCount) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.loteId = Objects.requireNonNull(loteId, "loteId is required");
        this.clienteDocumento = Objects.requireNonNull(clienteDocumento, "clienteDocumento is required");
        this.valorTotal = validateValorTotal(valorTotal);
        this.status = Objects.requireNonNull(status, "status is required");
        if (retryCount < 0) {
            throw new IllegalArgumentException("retryCount must be >= 0");
        }
        this.retryCount = retryCount;
    }

    public static Fatura createPending(UUID id, UUID loteId, String clienteDocumento, BigDecimal valorTotal) {
        return new Fatura(id, loteId, clienteDocumento, valorTotal, FaturaStatus.PENDENTE, 0);
    }

    public void solicitarPagamento() {
        if (status != FaturaStatus.PENDENTE && status != FaturaStatus.RECUSADO) {
            throw new IllegalStateException("Only PENDENTE or RECUSADO faturas can become SOLICITADO");
        }
        this.status = FaturaStatus.SOLICITADO;
    }

    public void marcarRecusado() {
        if (status != FaturaStatus.SOLICITADO) {
            throw new IllegalStateException("Only SOLICITADO faturas can become RECUSADO");
        }
        this.status = FaturaStatus.RECUSADO;
    }

    public FaturaStatus registrarTentativaRecusada(FaturaRetryPolicy retryPolicy) {
        if (status != FaturaStatus.RECUSADO) {
            throw new IllegalStateException("Retry handling only applies to RECUSADO faturas");
        }

        this.retryCount = retryPolicy.nextAttempt(this.retryCount);
        this.status = retryPolicy.nextStatusAfterRefusal(this.retryCount);
        return this.status;
    }

    public void moverParaProblema() {
        this.status = FaturaStatus.PROBLEMA;
    }

    public UUID getId() {
        return id;
    }

    public UUID getLoteId() {
        return loteId;
    }

    public String getClienteDocumento() {
        return clienteDocumento;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public FaturaStatus getStatus() {
        return status;
    }

    public int getRetryCount() {
        return retryCount;
    }

    private BigDecimal validateValorTotal(BigDecimal valorTotal) {
        BigDecimal value = Objects.requireNonNull(valorTotal, "valorTotal is required");
        if (value.signum() <= 0) {
            throw new IllegalArgumentException("valorTotal must be > 0");
        }
        return value;
    }
}
