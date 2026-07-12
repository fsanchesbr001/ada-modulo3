package com.fabriciosanches.adamodulo3.faturas.adapter.out.messaging;

import java.math.BigDecimal;

public record PagarEventPayload(
        String eventId,
        String faturaId,
        String loteId,
        BigDecimal valorTotal,
        String requestedAt,
        String traceId,
        String authorizationSubject) {
}
