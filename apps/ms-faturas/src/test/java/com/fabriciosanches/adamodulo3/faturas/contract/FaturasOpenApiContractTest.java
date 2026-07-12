package com.fabriciosanches.adamodulo3.faturas.contract;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FaturasOpenApiContractTest {

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
    void faturasOpenApiContractMustDeclareCoreEndpoints() throws IOException {
        Path contractPath = repoRoot().resolve(Path.of(".specs", "openapi", "ms-faturas.yaml"));
        assertTrue(Files.exists(contractPath), "OpenAPI contract file for ms-faturas must exist");

        String yaml = Files.readString(contractPath);
        assertAll(
            () -> assertTrue(yaml.contains("openapi: 3.0.3"), "Contract must declare supported OpenAPI version"),
                () -> assertTrue(yaml.contains("/api/v1/faturas/lote"), "Contract must declare POST /api/v1/faturas/lote"),
            () -> assertTrue(yaml.contains("post:"), "Contract must define POST operations"),
            () -> assertTrue(yaml.contains("\"202\":"), "Contract must declare acceptance responses for write operations"),
                () -> assertTrue(yaml.contains("/api/v1/faturas/{id}/pagamentos"), "Contract must declare POST /api/v1/faturas/{id}/pagamentos"),
            () -> assertTrue(yaml.contains("name: id"), "Contract must require path id parameter for item operations"),
            () -> assertTrue(yaml.contains("/api/v1/faturas/{id}"), "Contract must declare GET /api/v1/faturas/{id}"),
            () -> assertTrue(yaml.contains("\"200\":"), "Contract must declare 200 response for fatura read operation"));
    }
}
