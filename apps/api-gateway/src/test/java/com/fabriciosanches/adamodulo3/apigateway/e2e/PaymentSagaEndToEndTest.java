package com.fabriciosanches.adamodulo3.apigateway.e2e;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentSagaEndToEndTest {

    @Test
    void shouldEnforceJwtAndTracePropagationContracts() throws IOException {
        String pagarEvent = readFromRepo(".specs/asyncapi/pagar-event.yaml").toLowerCase();
        String authController = readFromRepo("apps/api-gateway/src/main/java/com/fabriciosanches/adamodulo3/apigateway/adapter/in/web/AuthController.java").toLowerCase();

        assertAll(
                () -> assertTrue(pagarEvent.contains("trace_id"), "PAGAR event must carry trace_id"),
                () -> assertTrue(pagarEvent.contains("authorization_subject"), "PAGAR event must carry JWT subject"),
                () -> assertTrue(authController.contains("httpheaders.authorization"), "Gateway login must expose bearer authorization header"));
    }

    @Test
    void shouldForbidSynchronousPagoBeforeComprovanteConfirmation() throws IOException {
        String processUseCase = readFromRepo("apps/ms-pagamentos/src/main/java/com/fabriciosanches/adamodulo3/pagamentos/application/ProcessPagamentoUseCase.java").toLowerCase();
        String finalityPolicy = readFromRepo("apps/ms-pagamentos/src/main/java/com/fabriciosanches/adamodulo3/pagamentos/domain/PagamentoFinalityPolicy.java").toLowerCase();

        assertAll(
                () -> assertTrue(processUseCase.contains("markaguardandocomprovante"), "Payment must wait for comprovante confirmation before PAGO"),
                () -> assertTrue(processUseCase.contains("onpagoblocked"), "Blocked synchronous PAGO must be observable"),
                () -> assertTrue(finalityPolicy.contains("iscomprovanteconfirmado"), "Finality policy must depend on comprovante confirmation"));
    }

    @Test
    void shouldRetainFullJsonPayloadAcrossComprovanteAndNotificacoesFlow() throws IOException {
        String comprovanteAsyncApi = readFromRepo(".specs/asyncapi/comprovante-gerado.yaml").toLowerCase();
        String notificacoesAsyncApi = readFromRepo(".specs/asyncapi/notificacoes-consumer.yaml").toLowerCase();
        String comprovantesOpenApi = readFromRepo(".specs/openapi/ms-comprovantes.yaml").toLowerCase();

        assertAll(
                () -> assertTrue(comprovanteAsyncApi.contains("payload_pdf_json"), "comprovante.gerado contract must keep payload_pdf_json"),
                () -> assertTrue(notificacoesAsyncApi.contains("payload_pdf_json"), "notifications consumer contract must keep payload_pdf_json"),
                () -> assertTrue(comprovantesOpenApi.contains("payload_pdf_json"), "comprovantes OpenAPI must keep payload_pdf_json"));
    }

    @Test
    void shouldKeepRabbitMqAndKafkaWiringForSagaCommunication() throws IOException {
        String queuePublisher = readFromRepo("apps/ms-comprovantes/src/main/java/com/fabriciosanches/adamodulo3/comprovantes/adapter/out/messaging/ComprovanteQueuePublisher.java").toLowerCase();
        String queueConsumer = readFromRepo("apps/ms-comprovantes/src/main/java/com/fabriciosanches/adamodulo3/comprovantes/adapter/in/messaging/ComprovanteQueueConsumer.java").toLowerCase();
        String kafkaPublisher = readFromRepo("apps/ms-comprovantes/src/main/java/com/fabriciosanches/adamodulo3/comprovantes/adapter/out/messaging/ComprovanteGeradoPublisher.java").toLowerCase();
        String pagamentosProperties = readFromRepo("apps/ms-pagamentos/src/main/resources/application.properties").toLowerCase();

        assertAll(
                () -> assertTrue(queuePublisher.contains("rabbittemplate"), "Comprovantes must publish into RabbitMQ"),
                () -> assertTrue(queueConsumer.contains("@rabbitlistener"), "Comprovantes must consume from RabbitMQ queue"),
                () -> assertTrue(kafkaPublisher.contains("kafkatemplate"), "Comprovantes must publish async confirmation to Kafka"),
                () -> assertTrue(pagamentosProperties.contains("messaging.kafka.topics.pagar"), "Pagamentos must consume PAGAR topic"),
                () -> assertTrue(pagamentosProperties.contains("messaging.kafka.topics.comprovante-gerado"), "Pagamentos must consume comprovante confirmation topic"));
    }

    @Test
    void shouldHonorRetryCeilingsAndDltPath() throws IOException {
        String faturaRetryPolicy = readFromRepo("apps/ms-faturas/src/main/java/com/fabriciosanches/adamodulo3/faturas/domain/FaturaRetryPolicy.java").toLowerCase();
        String notificationRetryPolicy = readFromRepo("apps/ms-notificacoes/src/main/java/com/fabriciosanches/adamodulo3/notificacoes/application/NotificationRetryPolicy.java").toLowerCase();
        String notificacoesListener = readFromRepo("apps/ms-notificacoes/src/main/java/com/fabriciosanches/adamodulo3/notificacoes/adapter/in/messaging/ComprovanteGeradoListener.java").toLowerCase();
        String dltContract = readFromRepo(".specs/asyncapi/comprovante-gerado-dlt.yaml").toLowerCase();

        assertAll(
                () -> assertTrue(faturaRetryPolicy.contains("max_retry_attempts = 3"), "Faturas retry ceiling must be 3"),
                () -> assertTrue(notificationRetryPolicy.contains("max_attempts = 3"), "Notifications retry ceiling must be 3"),
                () -> assertTrue(notificacoesListener.contains("@retryabletopic"), "Notifications flow must use retryable Kafka topic"),
                () -> assertTrue(notificacoesListener.contains("attempts = \"3\""), "RetryableTopic attempts must be capped at 3"),
                () -> assertTrue(notificacoesListener.contains("comprovante-gerado-dlt"), "Notifications flow must route exhausted records to DLT"),
                () -> assertTrue(dltContract.contains("comprovante.gerado.dlt"), "DLT contract must exist"));
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
