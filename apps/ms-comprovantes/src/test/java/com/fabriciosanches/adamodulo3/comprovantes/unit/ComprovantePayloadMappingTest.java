package com.fabriciosanches.adamodulo3.comprovantes.unit;

import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComprovantePayloadMappingTest {

    @Test
    void mappingMustKeepNestedJsonStructureUntouched() {
        Map<String, Object> source = Map.of(
                "payload_pdf_json", Map.of("arquivo", "base64", "meta", Map.of("origem", "api")),
                "trace_id", "trace-123");

        Map<String, Object> mapped = FakePayloadMapper.copy(source);

        assertEquals(source, mapped);
        assertTrue(((Map<?, ?>) mapped.get("payload_pdf_json")).containsKey("meta"));
    }

    private static final class FakePayloadMapper {
        private static Map<String, Object> copy(Map<String, Object> source) {
            return Map.copyOf(source);
        }
    }
}
