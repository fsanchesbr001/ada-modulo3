package com.fabriciosanches.adamodulo3.notificacoes.application.model;

import java.util.Map;

public record ComprovanteGeradoDltRecord(
        String id,
        String traceId,
        Map<String, Object> payloadPdfJson,
        int attempts,
        String error) {
}
