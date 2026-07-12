package com.fabriciosanches.adamodulo3.backoffice.contract;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProblemaFaturaRoutingContractTest {

    private static Path repoRoot() {
        Path current = Path.of("").toAbsolutePath();
        for (int i = 0; i < 8 && current != null; i++) {
            if (Files.exists(current.resolve(".specs"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Could not locate repository root (.specs)");
    }

    @Test
    void problemaRoutingAsyncApiContractMustDeclareFr040Fields() throws IOException {
        Path asyncApiPath = repoRoot().resolve(Path.of(".specs", "asyncapi", "problema-fatura-routing.yaml"));
        assertTrue(Files.exists(asyncApiPath), "AsyncAPI contract file for problema-fatura-routing must exist");

        String yaml = Files.readString(asyncApiPath).toLowerCase();
        assertAll(
                () -> assertTrue(yaml.contains("asyncapi:"), "Contract must declare AsyncAPI document version"),
                () -> assertTrue(yaml.contains("channels:"), "Contract must declare messaging channels"),
                () -> assertTrue(yaml.contains("problema"), "Contract must describe problema routing channel semantics"),
                () -> assertTrue(yaml.contains("idempotency_key"), "Contract must define idempotency_key for FR-040"),
                () -> assertTrue(yaml.contains("trace_id"), "Contract must define trace_id propagation"),
                () -> assertTrue(yaml.contains("retry_count_final"), "Contract must define exhausted retry counter"),
                () -> assertTrue(yaml.contains("payload_contexto"), "Contract must define payload_contexto persistence field"));
    }
}
