package com.fabriciosanches.adamodulo3.pagamentos.application.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record PagarEvent(
        String eventId,
        String faturaId,
        String loteId,
        BigDecimal valorTotal,
        Instant requestedAt,
        String traceId,
        String authorizationSubject) {

    public static PagarEvent fromPayload(Map<String, Object> payload, String traceIdHeader, String authorizationSubjectHeader) {
        String traceId = traceIdHeader == null || traceIdHeader.isBlank()
                ? String.valueOf(payload.getOrDefault("trace_id", ""))
                : traceIdHeader;
        String authorizationSubject = authorizationSubjectHeader == null || authorizationSubjectHeader.isBlank()
                ? String.valueOf(payload.getOrDefault("authorization_subject", ""))
                : authorizationSubjectHeader;

        return new PagarEvent(
                String.valueOf(payload.get("event_id")),
                String.valueOf(payload.get("fatura_id")),
                String.valueOf(payload.get("lote_id")),
                new BigDecimal(String.valueOf(payload.get("valor_total"))),
                Instant.parse(String.valueOf(payload.get("requested_at"))),
                traceId,
                authorizationSubject);
    }
}
