package com.fabriciosanches.adamodulo3.contracttest;

import java.nio.file.Path;

public final class AsyncApiContractVerifier {

    private AsyncApiContractVerifier() {}

    public static void assertContractExists(Path contractPath) {
        if (contractPath == null || !contractPath.toFile().exists()) {
            throw new IllegalStateException("AsyncAPI contract not found: " + contractPath);
        }
    }
}
