package com.fabriciosanches.adamodulo3.comprovantes.adapter.out.messaging;

import java.util.Map;

public record ComprovanteQueueMessage(
        String id,
        String traceId,
        Map<String, Object> payloadPdfJson) {
}
