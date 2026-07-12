package com.fabriciosanches.adamodulo3.backoffice.pact;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProblemaFaturaRoutingPactTest {

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
    void problemaRoutingContractMustExposeMessagingPactSemantics() throws IOException {
        Path asyncApiPath = repoRoot().resolve(Path.of(".specs", "asyncapi", "problema-fatura-routing.yaml"));
        assertTrue(Files.exists(asyncApiPath), "AsyncAPI contract file for problema-fatura-routing must exist");

        String yaml = Files.readString(asyncApiPath).toLowerCase();
        assertAll(
                () -> assertTrue(yaml.contains("subscribe") || yaml.contains("publish"), "Contract must declare message direction for provider/consumer pact"),
                () -> assertTrue(yaml.contains("message"), "Contract must define a message section"),
                () -> assertTrue(yaml.contains("fatura_id"), "Contract must include fatura_id for correlation"),
                () -> assertTrue(yaml.contains("idempotency_key"), "Contract must include idempotency key semantics"),
                () -> assertTrue(yaml.contains("trace_id"), "Contract must include trace propagation semantics"),
                () -> assertTrue(yaml.contains("motivo"), "Contract must include refusal reason for backoffice intake"));
    }
}
