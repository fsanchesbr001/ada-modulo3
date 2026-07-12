package com.fabriciosanches.adamodulo3.comprovantes.contract;

import com.fabriciosanches.adamodulo3.contracttest.AsyncApiContractVerifier;
import com.fabriciosanches.adamodulo3.contracttest.OpenApiContractVerifier;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ComprovantesProviderContractVerificationTest {

    @Test
    void providerContractsConfiguredInPomMustExist() {
        OpenApiContractVerifier.assertContractExists(Path.of(System.getProperty("openapi.comprovantes.contract")));
        OpenApiContractVerifier.assertContractExists(Path.of(System.getProperty("openapi.pagamentos.contract")));
        AsyncApiContractVerifier.assertContractExists(Path.of(System.getProperty("asyncapi.comprovante.gerado.contract")));
    }
}
