package com.fabriciosanches.adamodulo3.faturas.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TracePropagationSc006QualityGateTest {

    @Test
    void mustKeepTraceIdOnKafkaHeadersAndStructuredLogs() throws IOException {
        String pagarPublisher = readFromRepo("apps/ms-faturas/src/main/java/com/fabriciosanches/adamodulo3/faturas/adapter/out/messaging/PagarEventPublisher.java").toLowerCase();
        String problemaPublisher = readFromRepo("apps/ms-faturas/src/main/java/com/fabriciosanches/adamodulo3/faturas/adapter/out/messaging/ProblemaFaturaPublisher.java").toLowerCase();
        String scheduler = readFromRepo("apps/ms-faturas/src/main/java/com/fabriciosanches/adamodulo3/faturas/adapter/in/scheduler/FaturaRetryScheduler.java").toLowerCase();

        assertAll(
                () -> assertTrue(pagarPublisher.contains("trace_id"), "PAGAR publisher must propagate trace_id header"),
                () -> assertTrue(problemaPublisher.contains("trace_id"), "Problema routing publisher must propagate trace_id"),
                () -> assertTrue(problemaPublisher.contains("mdcenricher.put(\"trace_id\""), "Problema routing logs must include trace_id in MDC"),
                () -> assertTrue(scheduler.contains("ensuretraceidorigem"), "Retry scheduler must ensure and reuse trace_id"));
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
