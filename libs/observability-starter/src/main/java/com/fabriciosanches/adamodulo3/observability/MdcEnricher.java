package com.fabriciosanches.adamodulo3.observability;

import org.slf4j.MDC;

public final class MdcEnricher {

    private MdcEnricher() {}

    public static void put(String key, String value) {
        if (value != null && !value.isBlank()) {
            MDC.put(key, value);
        }
    }

    public static void clear() {
        MDC.clear();
    }
}
