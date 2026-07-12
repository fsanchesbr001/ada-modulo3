package com.fabriciosanches.adamodulo3.faturas.adapter.in.web.dto;

import com.fabriciosanches.adamodulo3.faturas.application.model.GetFaturaResult;
import java.math.BigDecimal;
import java.util.UUID;

public record FaturaResponse(
        UUID id,
        UUID loteId,
        String clienteDocumento,
        BigDecimal valorTotal,
        String status,
        int retryCount) {

    public static FaturaResponse from(GetFaturaResult result) {
        return new FaturaResponse(
                result.id(),
                result.loteId(),
                result.clienteDocumento(),
                result.valorTotal(),
                result.status().name(),
                result.retryCount());
    }
}
