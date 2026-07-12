package com.fabriciosanches.adamodulo3.faturas.application.model;

import com.fabriciosanches.adamodulo3.faturas.domain.FaturaStatus;
import java.math.BigDecimal;
import java.util.UUID;

public record GetFaturaResult(
        UUID id,
        UUID loteId,
        String clienteDocumento,
        BigDecimal valorTotal,
        FaturaStatus status,
        int retryCount) {
}
