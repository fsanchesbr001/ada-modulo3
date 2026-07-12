package com.fabriciosanches.adamodulo3.faturas.pact;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FaturasHttpPactProviderTest {

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
    void httpContractPreconditionsForPactMustBePresent() throws IOException {
        Path contractPath = repoRoot().resolve(Path.of(".specs", "openapi", "ms-faturas.yaml"));
        assertTrue(Files.exists(contractPath), "OpenAPI contract file for ms-faturas must exist");

        String yaml = Files.readString(contractPath);
        assertAll(
                () -> assertTrue(yaml.contains("openapi:"), "Contract must be an OpenAPI document"),
                () -> assertTrue(yaml.contains("/api/v1/faturas/lote"), "Pact precondition: lote endpoint must exist"),
                () -> assertTrue(yaml.contains("/api/v1/faturas/{id}/pagamentos"), "Pact precondition: pagamento request endpoint must exist"),
                () -> assertTrue(yaml.contains("/api/v1/faturas/{id}"), "Pact precondition: fatura lookup endpoint must exist"));
    }
}
