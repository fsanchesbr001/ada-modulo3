package com.fabriciosanches.adamodulo3.notificacoes.pact;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificacoesMessagingPactTest {

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
    void messagingPactContractsMustCoverConsumerAndDltRoutes() throws IOException {
        String consumerYaml = Files.readString(repoRoot().resolve(Path.of(".specs", "asyncapi", "notificacoes-consumer.yaml"))).toLowerCase();
        String dltYaml = Files.readString(repoRoot().resolve(Path.of(".specs", "asyncapi", "comprovante-gerado-dlt.yaml"))).toLowerCase();

        assertAll(
                () -> assertTrue(consumerYaml.contains("comprovante.gerado.topic")),
                () -> assertTrue(consumerYaml.contains("payload_pdf_json")),
                () -> assertTrue(consumerYaml.contains("trace_id")),
                () -> assertTrue(dltYaml.contains("comprovante.gerado.dlt")),
                () -> assertTrue(dltYaml.contains("attempts")),
                () -> assertTrue(dltYaml.contains("error")));
    }
}
