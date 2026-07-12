package com.fabriciosanches.adamodulo3.notificacoes.contract;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificacoesContractTest {

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
    void notificacoesConsumerAndDltContractsMustDeclareChannelsAndPayload() throws IOException {
        String consumerYaml = Files.readString(repoRoot().resolve(Path.of(".specs", "asyncapi", "notificacoes-consumer.yaml"))).toLowerCase();
        String dltYaml = Files.readString(repoRoot().resolve(Path.of(".specs", "asyncapi", "comprovante-gerado-dlt.yaml"))).toLowerCase();

        assertAll(
                () -> assertTrue(consumerYaml.contains("comprovante.gerado.topic"), "Consumer contract must declare comprovante.gerado.topic"),
                () -> assertTrue(consumerYaml.contains("subscribe"), "Consumer contract must be subscribe semantic"),
                () -> assertTrue(consumerYaml.contains("payload_pdf_json"), "Consumer contract must retain full payload"),
                () -> assertTrue(consumerYaml.contains("trace_id"), "Consumer contract must include trace_id"),
                () -> assertTrue(dltYaml.contains("comprovante.gerado.dlt"), "DLT contract must declare comprovante.gerado.DLT channel"),
                () -> assertTrue(dltYaml.contains("attempts"), "DLT contract must include attempts metadata"),
                () -> assertTrue(dltYaml.contains("error"), "DLT contract must include error metadata"));
    }
}
