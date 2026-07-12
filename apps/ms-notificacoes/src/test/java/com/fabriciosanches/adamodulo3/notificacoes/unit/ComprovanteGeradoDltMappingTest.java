package com.fabriciosanches.adamodulo3.notificacoes.unit;

import com.fabriciosanches.adamodulo3.notificacoes.adapter.in.messaging.ComprovanteGeradoListener;
import com.fabriciosanches.adamodulo3.notificacoes.adapter.out.notification.ComprovanteGeradoDltPublisher;
import com.fabriciosanches.adamodulo3.notificacoes.adapter.out.notification.NotificationSender;
import com.fabriciosanches.adamodulo3.notificacoes.application.NotificationRetryPolicy;
import com.fabriciosanches.adamodulo3.notificacoes.application.ProcessNotificacaoUseCase;
import com.fabriciosanches.adamodulo3.notificacoes.application.model.ComprovanteGeradoDltRecord;
import com.fabriciosanches.adamodulo3.notificacoes.application.model.ComprovanteGeradoMessage;
import com.fabriciosanches.adamodulo3.notificacoes.config.NotificacoesObservability;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ComprovanteGeradoDltMappingTest {

    @Test
    void consumeDltMustMapPayloadWithAttemptsAndError() {
        CapturingDltPublisher dltPublisher = new CapturingDltPublisher();
        ProcessNotificacaoUseCase useCase = new ProcessNotificacaoUseCase(new NoopSender(), observability());
        ComprovanteGeradoListener listener = new ComprovanteGeradoListener(
                useCase,
                dltPublisher,
                observability(),
                new NotificationRetryPolicy());

        listener.consumeDlt(Map.of(
                        "id", "cmp-1",
                        "trace_id", "trace-1",
                        "payload_pdf_json", Map.of("x", "y")),
                "trace-1",
                "boom");

        ComprovanteGeradoDltRecord record = dltPublisher.last;
        assertEquals("cmp-1", record.id());
        assertEquals("trace-1", record.traceId());
        assertEquals(3, record.attempts());
        assertEquals("boom", record.error());
        assertEquals("y", record.payloadPdfJson().get("x"));
    }

    private static NotificacoesObservability observability() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        return new NotificacoesObservability(
                registry.counter("notificacoes_consumer_throughput_total"),
                registry.counter("notificacoes_retries_total"),
                registry.counter("notificacoes_dlt_total"));
    }

    private static final class NoopSender implements NotificationSender {
        @Override
        public void send(ComprovanteGeradoMessage message) {
        }
    }

    private static final class CapturingDltPublisher implements ComprovanteGeradoDltPublisher {
        private ComprovanteGeradoDltRecord last;

        @Override
        public void publish(ComprovanteGeradoDltRecord record) {
            this.last = record;
        }
    }
}
