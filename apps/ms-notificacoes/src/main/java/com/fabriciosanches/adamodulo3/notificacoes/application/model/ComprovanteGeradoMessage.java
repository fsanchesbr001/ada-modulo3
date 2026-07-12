package com.fabriciosanches.adamodulo3.notificacoes.application.model;

import java.util.Map;

public record ComprovanteGeradoMessage(
        String id,
        String traceId,
        Map<String, Object> payloadPdfJson) {
}
