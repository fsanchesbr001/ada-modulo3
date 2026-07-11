package com.fabriciosanches.adamodulo3.observability;

import java.util.Map;

public final class MessagingTracePropagator {

    private MessagingTracePropagator() {}

    public static void propagate(Map<String, Object> headers, String traceId) {
        if (headers != null && traceId != null && !traceId.isBlank()) {
            headers.put(TraceIdFilter.TRACE_ID_HEADER, traceId);
        }
    }
}
