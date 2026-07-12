package com.fabriciosanches.adamodulo3.apigateway.e2e;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TracePropagationSc006QualityGateTest {

    @Test
    void mustKeepTraceIdAcrossGatewayAndPagarContract() throws IOException {
        String traceGatewayFilter = readFromRepo("apps/api-gateway/src/main/java/com/fabriciosanches/adamodulo3/apigateway/config/TracePropagationGatewayFilter.java").toLowerCase();
        String pagarContract = readFromRepo(".specs/asyncapi/pagar-event.yaml").toLowerCase();

        assertAll(
                () -> assertTrue(traceGatewayFilter.contains("traceidfilter.trace_id_header"), "Gateway filter must propagate trace_id header"),
                () -> assertTrue(pagarContract.contains("trace_id"), "PAGAR contract must require trace_id"),
                () -> assertTrue(pagarContract.contains("authorization_subject"), "PAGAR contract must carry JWT subject"));
    }

    private static String readFromRepo(String relativePath) throws IOException {
        return Files.readString(repoRoot().resolve(relativePath));
    }

    private static Path repoRoot() {
        Path current = Path.of("").toAbsolutePath();
        for (int i = 0; i < 8 && current != null; i++) {
            if (Files.exists(current.resolve(".specs")) && Files.exists(current.resolve("apps"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Could not locate repository root");
    }
}
