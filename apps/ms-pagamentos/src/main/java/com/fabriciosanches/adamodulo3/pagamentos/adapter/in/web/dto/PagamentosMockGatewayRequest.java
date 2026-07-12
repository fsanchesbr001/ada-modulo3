package com.fabriciosanches.adamodulo3.pagamentos.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PagamentosMockGatewayRequest(
        @NotBlank String eventId,
        @NotBlank String faturaId,
        @NotBlank String loteId,
        @NotNull BigDecimal valorTotal,
        @NotBlank String traceId,
        @NotBlank String authorizationSubject) {
}
