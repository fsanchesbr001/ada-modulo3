package com.fabriciosanches.adamodulo3.faturas.integration;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FaturaRetrySchedulerTraceIdIntegrationTest {

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
    void schedulerTraceIdPreconditionsMustBeInPlace() {
        Path observabilityStarterPath = repoRoot().resolve(Path.of(
                "libs",
                "observability-starter",
                "src",
                "main",
                "java",
                "com",
                "fabriciosanches",
                "adamodulo3",
                "observability",
                "TraceIdFilter.java"));
        Path routingContractPath = repoRoot().resolve(Path.of(
                ".specs",
                "asyncapi",
                "problema-fatura-routing.yaml"));

        assertAll(
                () -> assertTrue(
                        Files.exists(observabilityStarterPath),
                        "Trace infrastructure precondition: TraceIdFilter must exist"),
                () -> assertTrue(
                        Files.exists(routingContractPath),
                        "Routing precondition: problema-fatura routing contract must exist"));
    }
}
