package com.fabriciosanches.adamodulo3.faturas.pact;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PagarEventMessagePactTest {

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
    void pagarEventAsyncApiContractMustExistForMessagingPact() throws IOException {
        Path asyncApiPath = repoRoot().resolve(Path.of(".specs", "asyncapi", "pagar-event.yaml"));
        assertTrue(Files.exists(asyncApiPath), "AsyncAPI contract file for PAGAR event must exist");

        String yaml = Files.readString(asyncApiPath);
        assertAll(
            () -> assertTrue(yaml.contains("asyncapi: 2.6.0"), "Contract must be an AsyncAPI 2.6.0 document"),
            () -> assertTrue(yaml.contains("channels:"), "Contract must declare messaging channels"),
            () -> assertTrue(yaml.contains("pagar:"), "Contract must declare the PAGAR channel"),
            () -> assertTrue(yaml.contains("publish:"), "Contract must declare publisher semantics for PAGAR"),
                () -> assertTrue(yaml.toLowerCase().contains("pagar"), "Contract must describe PAGAR semantics"),
            () -> assertTrue(yaml.toLowerCase().contains("fatura_id"), "Contract must include fatura correlation field"),
            () -> assertTrue(yaml.toLowerCase().contains("trace_id"), "Contract must include trace_id propagation expectations"));
    }
}
