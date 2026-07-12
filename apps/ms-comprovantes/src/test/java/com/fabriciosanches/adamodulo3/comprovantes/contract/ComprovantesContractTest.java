package com.fabriciosanches.adamodulo3.comprovantes.contract;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComprovantesContractTest {

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
    void comprovantesOpenApiContractMustDeclarePostAndGetEndpoints() throws IOException {
        Path contractPath = repoRoot().resolve(Path.of(".specs", "openapi", "ms-comprovantes.yaml"));
        assertTrue(Files.exists(contractPath), "OpenAPI contract file for ms-comprovantes must exist");

        String yaml = Files.readString(contractPath);
        assertAll(
                () -> assertTrue(yaml.contains("openapi: 3.0.3"), "Contract must declare supported OpenAPI version"),
                () -> assertTrue(yaml.contains("/api/v1/comprovantes"), "Contract must declare POST /api/v1/comprovantes"),
                () -> assertTrue(yaml.contains("/api/v1/comprovantes/{id}"), "Contract must declare GET /api/v1/comprovantes/{id}"),
                () -> assertTrue(yaml.contains("post:"), "Contract must define POST operation for comprovantes creation"),
                () -> assertTrue(yaml.contains("get:"), "Contract must define GET operation for comprovantes retrieval"),
                () -> assertTrue(yaml.contains("\"202\":"), "Contract must define 202 Accepted response for POST"),
                () -> assertTrue(yaml.contains("\"200\":"), "Contract must define 200 response for GET"));
    }

    @Test
    void comprovanteAsyncApiContractMustDeclareQueueMessageSchema() throws IOException {
        Path contractPath = repoRoot().resolve(Path.of(".specs", "asyncapi", "comprovante-gerado.yaml"));
        assertTrue(Files.exists(contractPath), "AsyncAPI contract file for comprovante-gerado must exist");

        String yaml = Files.readString(contractPath);
        assertAll(
                () -> assertTrue(yaml.contains("asyncapi: 2.6.0"), "Contract must declare supported AsyncAPI version"),
                () -> assertTrue(yaml.contains("comprovante.queue"), "Contract must declare comprovante queue channel"),
                () -> assertTrue(yaml.contains("comprovante.gerado.topic"), "Contract must declare comprovante generated topic"),
                () -> assertTrue(yaml.contains("ComprovanteQueueMessage"), "Contract must declare ComprovanteQueueMessage"),
                () -> assertTrue(yaml.toLowerCase().contains("trace_id"), "Contract must include trace_id propagation field"),
                () -> assertTrue(yaml.toLowerCase().contains("payload_pdf_json"), "Contract must include payload_pdf_json field"));
    }
}
