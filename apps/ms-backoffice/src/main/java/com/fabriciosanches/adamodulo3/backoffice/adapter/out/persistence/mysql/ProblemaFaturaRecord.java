package com.fabriciosanches.adamodulo3.backoffice.adapter.out.persistence.mysql;

public record ProblemaFaturaRecord(
        String idempotencyKey,
        String faturaId,
        String motivo,
        int retryCountFinal,
        String payloadContexto,
        String traceId) {
}
