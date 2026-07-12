package com.fabriciosanches.adamodulo3.comprovantes.pact;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComprovantesHttpPactProviderTest {

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
    void comprovantesOpenApiContractMustSupportHttpPactProviderFlows() throws IOException {
        Path contractPath = repoRoot().resolve(Path.of(".specs", "openapi", "ms-comprovantes.yaml"));
        assertTrue(Files.exists(contractPath), "OpenAPI contract file for ms-comprovantes must exist");

        String yaml = Files.readString(contractPath);
        assertAll(
                () -> assertTrue(yaml.contains("/api/v1/comprovantes"), "Contract must declare POST endpoint for provider pact"),
                () -> assertTrue(yaml.contains("/api/v1/comprovantes/{id}"), "Contract must declare GET endpoint for provider pact"),
                () -> assertTrue(yaml.contains("\"202\":"), "Contract must expose 202 for asynchronous POST acceptance"),
                () -> assertTrue(yaml.contains("\"200\":"), "Contract must expose 200 for GET retrieval success"),
                () -> assertTrue(yaml.contains("payload_pdf_json"), "Contract must define payload_pdf_json in HTTP payload model"));
    }
}
