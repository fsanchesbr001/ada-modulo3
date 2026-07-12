package com.fabriciosanches.adamodulo3.pagamentos.contract;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PagamentosContractTest {

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
    void pagamentosOpenApiContractMustDeclareMockGatewayEndpoint() throws IOException {
        Path contractPath = repoRoot().resolve(Path.of(".specs", "openapi", "ms-pagamentos.yaml"));
        assertTrue(Files.exists(contractPath), "OpenAPI contract file for ms-pagamentos must exist");

        String yaml = Files.readString(contractPath);
        assertAll(
                () -> assertTrue(yaml.contains("openapi: 3.0.3"), "Contract must declare supported OpenAPI version"),
                () -> assertTrue(
                        yaml.contains("/api/v1/pagamentos/mock/gateway/lote"),
                        "Contract must declare POST /api/v1/pagamentos/mock/gateway/lote"),
                () -> assertTrue(yaml.contains("post:"), "Contract must define POST operation for mock gateway lote"),
                () -> assertTrue(yaml.contains("\"202\":"), "Contract must declare 202 response for mock gateway lote"));
    }

    @Test
    void pagamentosAsyncApiContractsMustDeclarePagarAndComprovanteConfirmationChannels() throws IOException {
        Path pagarEventPath = repoRoot().resolve(Path.of(".specs", "asyncapi", "pagar-event.yaml"));
        Path comprovanteGeradoPath = repoRoot().resolve(Path.of(".specs", "asyncapi", "comprovante-gerado.yaml"));

        assertTrue(Files.exists(pagarEventPath), "AsyncAPI contract file for PAGAR event must exist");
        assertTrue(Files.exists(comprovanteGeradoPath), "AsyncAPI contract file for comprovante-gerado must exist");

        String pagarYaml = Files.readString(pagarEventPath).toLowerCase();
        String comprovanteYaml = Files.readString(comprovanteGeradoPath).toLowerCase();

        assertAll(
                () -> assertTrue(pagarYaml.contains("asyncapi:"), "PAGAR contract must declare AsyncAPI document version"),
                () -> assertTrue(pagarYaml.contains("channels:"), "PAGAR contract must declare messaging channels"),
                () -> assertTrue(pagarYaml.contains("pagar"), "PAGAR contract must declare pagar channel"),
                () -> assertTrue(pagarYaml.contains("trace_id"), "PAGAR contract must include trace_id propagation"),
                () -> assertTrue(
                        comprovanteYaml.contains("comprovante.gerado.topic"),
                        "Comprovante contract must declare comprovante.gerado.topic confirmation channel"),
                () -> assertTrue(
                        comprovanteYaml.contains("payload_pdf_json"),
                        "Comprovante contract must include payload_pdf_json for confirmation handling"));
    }
}
