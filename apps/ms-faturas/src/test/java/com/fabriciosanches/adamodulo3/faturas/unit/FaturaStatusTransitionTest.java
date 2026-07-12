package com.fabriciosanches.adamodulo3.faturas.unit;

import com.fabriciosanches.adamodulo3.faturas.domain.Fatura;
import com.fabriciosanches.adamodulo3.faturas.domain.FaturaRetryPolicy;
import com.fabriciosanches.adamodulo3.faturas.domain.FaturaStatus;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FaturaStatusTransitionTest {

    @Test
    void solicitarPagamentoMustMovePendingToSolicitado() {
        Fatura fatura = Fatura.createPending(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "12345678901",
                new BigDecimal("50.00"),
                "trace-1");

        fatura.solicitarPagamento();

        assertEquals(FaturaStatus.SOLICITADO, fatura.getStatus());
    }

    @Test
    void marcarRecusadoMustOnlyWorkFromSolicitado() {
        Fatura fatura = new Fatura(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "12345678901",
                new BigDecimal("60.00"),
                "trace-2",
                FaturaStatus.SOLICITADO,
                0);

        fatura.marcarRecusado();

        assertEquals(FaturaStatus.RECUSADO, fatura.getStatus());
    }

    @Test
    void marcarRecusadoMustFailWhenStatusIsNotSolicitado() {
        Fatura fatura = Fatura.createPending(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "12345678901",
                new BigDecimal("60.00"),
                "trace-3");

        assertThrows(IllegalStateException.class, fatura::marcarRecusado);
    }

    @Test
    void registrarTentativaRecusadaMustIncrementRetryAndTransitionToProblemaAtCeiling() {
        Fatura fatura = new Fatura(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "12345678901",
                new BigDecimal("75.00"),
                "trace-4",
                FaturaStatus.RECUSADO,
                2);

        FaturaStatus next = fatura.registrarTentativaRecusada(new FaturaRetryPolicy());

        assertEquals(FaturaStatus.PROBLEMA, next);
        assertEquals(FaturaStatus.PROBLEMA, fatura.getStatus());
        assertEquals(3, fatura.getRetryCount());
    }

    @Test
    void registrarTentativaRecusadaMustFailWhenStatusIsNotRecusado() {
        Fatura fatura = Fatura.createPending(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "12345678901",
                new BigDecimal("75.00"),
                "trace-5");

        assertThrows(IllegalStateException.class, () -> fatura.registrarTentativaRecusada(new FaturaRetryPolicy()));
    }
}
