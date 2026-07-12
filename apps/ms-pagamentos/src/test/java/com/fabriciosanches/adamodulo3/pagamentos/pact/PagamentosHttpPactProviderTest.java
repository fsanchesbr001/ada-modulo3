package com.fabriciosanches.adamodulo3.pagamentos.pact;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PagamentosHttpPactProviderTest {

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
    void pagamentosOpenApiContractMustExposeMockGatewayLoteEndpointForHttpPact() throws IOException {
        Path openApiPath = repoRoot().resolve(Path.of(".specs", "openapi", "ms-pagamentos.yaml"));
        assertTrue(Files.exists(openApiPath), "OpenAPI contract file for ms-pagamentos must exist");

        String yaml = Files.readString(openApiPath);
        assertAll(
                () -> assertTrue(yaml.contains("/api/v1/pagamentos/mock/gateway/lote"), "Contract must expose mock gateway lote endpoint"),
                () -> assertTrue(yaml.contains("post:"), "Contract must include POST operation"),
                () -> assertTrue(yaml.contains("\"202\":"), "Contract must include 202 accepted response"));
    }
}
