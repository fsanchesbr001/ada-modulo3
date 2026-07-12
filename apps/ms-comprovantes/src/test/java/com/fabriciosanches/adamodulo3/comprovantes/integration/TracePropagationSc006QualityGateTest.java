package com.fabriciosanches.adamodulo3.comprovantes.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TracePropagationSc006QualityGateTest {

    @Test
    void mustPreserveTraceIdAcrossRabbitKafkaAndLogs() throws IOException {
        String queuePublisher = readFromRepo("apps/ms-comprovantes/src/main/java/com/fabriciosanches/adamodulo3/comprovantes/adapter/out/messaging/ComprovanteQueuePublisher.java").toLowerCase();
        String queueConsumer = readFromRepo("apps/ms-comprovantes/src/main/java/com/fabriciosanches/adamodulo3/comprovantes/adapter/in/messaging/ComprovanteQueueConsumer.java").toLowerCase();
        String kafkaPublisher = readFromRepo("apps/ms-comprovantes/src/main/java/com/fabriciosanches/adamodulo3/comprovantes/adapter/out/messaging/ComprovanteGeradoPublisher.java").toLowerCase();
        String observability = readFromRepo("apps/ms-comprovantes/src/main/java/com/fabriciosanches/adamodulo3/comprovantes/config/ComprovantesObservability.java").toLowerCase();

        assertAll(
                () -> assertTrue(queuePublisher.contains("setheader(\"trace_id\""), "Rabbit publisher must set trace_id header"),
                () -> assertTrue(queueConsumer.contains("payload.put(\"trace_id\""), "Rabbit consumer must keep trace_id in outbound payload"),
                () -> assertTrue(kafkaPublisher.contains("setheader(\"trace_id\""), "Kafka publisher must keep trace_id header"),
                () -> assertTrue(observability.contains("mdcenricher.put(\"trace_id\""), "Structured logs must include trace_id via MDC"));
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
