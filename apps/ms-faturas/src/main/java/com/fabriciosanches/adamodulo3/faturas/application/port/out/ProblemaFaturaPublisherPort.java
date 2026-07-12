package com.fabriciosanches.adamodulo3.faturas.application.port.out;

import java.util.UUID;

public interface ProblemaFaturaPublisherPort {

    void publishProblemaFatura(
            UUID faturaId,
            int retryCountFinal,
            String idempotencyKey,
            String traceId,
            String motivo,
            String payloadContexto);
}
