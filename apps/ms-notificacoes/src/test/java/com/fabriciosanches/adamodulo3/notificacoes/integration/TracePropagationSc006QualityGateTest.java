package com.fabriciosanches.adamodulo3.notificacoes.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TracePropagationSc006QualityGateTest {

    @Test
    void mustKeepTraceIdFromConsumerToDltAndLogs() throws IOException {
        String listener = readFromRepo("apps/ms-notificacoes/src/main/java/com/fabriciosanches/adamodulo3/notificacoes/adapter/in/messaging/ComprovanteGeradoListener.java").toLowerCase();
        String observability = readFromRepo("apps/ms-notificacoes/src/main/java/com/fabriciosanches/adamodulo3/notificacoes/config/NotificacoesObservability.java").toLowerCase();
        String asyncApiConsumer = readFromRepo(".specs/asyncapi/notificacoes-consumer.yaml").toLowerCase();
        String asyncApiDlt = readFromRepo(".specs/asyncapi/comprovante-gerado-dlt.yaml").toLowerCase();

        assertAll(
                () -> assertTrue(listener.contains("@header(name = \"trace_id\""), "Listener must read trace_id header"),
                () -> assertTrue(listener.contains("resolvedtrace"), "Listener must propagate resolved trace into DLT mapping"),
                () -> assertTrue(observability.contains("mdcenricher.put(\"trace_id\""), "Notifications logs must include trace_id in MDC"),
                () -> assertTrue(asyncApiConsumer.contains("trace_id"), "Notifications consumer contract must carry trace_id"),
                () -> assertTrue(asyncApiDlt.contains("trace_id"), "DLT contract must carry trace_id"));
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
