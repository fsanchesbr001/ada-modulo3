package com.fabriciosanches.adamodulo3.notificacoes.unit;

import com.fabriciosanches.adamodulo3.notificacoes.adapter.out.notification.NotificationSender;
import com.fabriciosanches.adamodulo3.notificacoes.application.ProcessNotificacaoUseCase;
import com.fabriciosanches.adamodulo3.notificacoes.application.model.ComprovanteGeradoMessage;
import com.fabriciosanches.adamodulo3.notificacoes.config.NotificacoesObservability;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProcessNotificacaoUseCaseTest {

    @Test
    void processMustOrchestrateSenderWithFullPayload() {
        CapturingSender sender = new CapturingSender();
        ProcessNotificacaoUseCase useCase = new ProcessNotificacaoUseCase(sender, observability());

        useCase.process(Map.of(
                "id", "cmp-1",
                "trace_id", "trace-1",
                "payload_pdf_json", Map.of("k", "v")), "trace-1", 1);

        assertEquals("cmp-1", sender.last.id());
        assertEquals("trace-1", sender.last.traceId());
        assertEquals("v", sender.last.payloadPdfJson().get("k"));
    }

    private static NotificacoesObservability observability() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        return new NotificacoesObservability(
                registry.counter("notificacoes_consumer_throughput_total"),
                registry.counter("notificacoes_retries_total"),
                registry.counter("notificacoes_dlt_total"));
    }

    private static final class CapturingSender implements NotificationSender {
        private ComprovanteGeradoMessage last;

        @Override
        public void send(ComprovanteGeradoMessage message) {
            this.last = message;
        }
    }
}
