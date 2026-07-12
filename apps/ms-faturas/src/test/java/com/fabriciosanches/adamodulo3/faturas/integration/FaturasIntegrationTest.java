package com.fabriciosanches.adamodulo3.faturas.integration;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FaturasIntegrationTest {

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
    void integrationPreconditionsMustExistForUs2Flow() {
        Path openApiPath = repoRoot().resolve(Path.of(".specs", "openapi", "ms-faturas.yaml"));
        Path asyncApiPath = repoRoot().resolve(Path.of(".specs", "asyncapi", "pagar-event.yaml"));
        assertAll(
            () -> assertTrue(Files.exists(openApiPath), "US2 integration precondition: OpenAPI contract must exist"),
            () -> assertTrue(Files.exists(asyncApiPath), "US2 integration precondition: AsyncAPI PAGAR contract must exist"));
    }
}
