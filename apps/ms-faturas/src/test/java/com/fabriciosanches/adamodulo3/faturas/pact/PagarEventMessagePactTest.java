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
                () -> assertTrue(yaml.contains("asyncapi:"), "Contract must be an AsyncAPI document"),
                () -> assertTrue(yaml.toLowerCase().contains("pagar"), "Contract must describe PAGAR semantics"),
                () -> assertTrue(yaml.toLowerCase().contains("trace_id"), "Contract must include trace_id propagation expectations"));
    }
}
