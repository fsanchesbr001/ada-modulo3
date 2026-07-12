package com.fabriciosanches.adamodulo3.comprovantes.pact;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComprovantesMessagingPactTest {

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
    void comprovanteAsyncApiContractMustSupportQueueAndKafkaMessagingPactFlows() throws IOException {
        Path contractPath = repoRoot().resolve(Path.of(".specs", "asyncapi", "comprovante-gerado.yaml"));
        assertTrue(Files.exists(contractPath), "AsyncAPI contract file for comprovante-gerado must exist");

        String yaml = Files.readString(contractPath);
        assertAll(
                () -> assertTrue(yaml.contains("comprovante.queue"), "Contract must include RabbitMQ queue publication channel"),
                () -> assertTrue(yaml.contains("comprovante.gerado.topic"), "Contract must include Kafka publication topic"),
                () -> assertTrue(yaml.contains("ComprovanteQueueMessage"), "Contract must include ComprovanteQueueMessage"),
                () -> assertTrue(yaml.toLowerCase().contains("trace_id"), "Contract must include trace propagation field"),
                () -> assertTrue(yaml.toLowerCase().contains("payload_pdf_json"), "Contract must include PDF payload field"));
    }
}
