package com.fabriciosanches.adamodulo3.contracttest;

import java.nio.file.Path;

public final class OpenApiContractVerifier {

    private OpenApiContractVerifier() {}

    public static void assertContractExists(Path contractPath) {
        if (contractPath == null || !contractPath.toFile().exists()) {
            throw new IllegalStateException("OpenAPI contract not found: " + contractPath);
        }
    }
}
