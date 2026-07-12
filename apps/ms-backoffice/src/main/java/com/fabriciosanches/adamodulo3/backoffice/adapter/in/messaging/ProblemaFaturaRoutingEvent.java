package com.fabriciosanches.adamodulo3.backoffice.adapter.in.messaging;

public record ProblemaFaturaRoutingEvent(
        String eventId,
        String idempotencyKey,
        String traceId,
        String faturaId,
        int retryCountFinal,
        String motivo,
        String payloadContexto,
        String routedAt) {
}
