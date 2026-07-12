package com.fabriciosanches.adamodulo3.faturas.adapter.out.messaging;

public record ProblemaFaturaRoutingPayload(
        String eventId,
        String idempotencyKey,
        String traceId,
        String faturaId,
        int retryCountFinal,
        String motivo,
        String payloadContexto,
        String routedAt) {
}
