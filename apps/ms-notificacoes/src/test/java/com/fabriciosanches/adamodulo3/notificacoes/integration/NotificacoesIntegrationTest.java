package com.fabriciosanches.adamodulo3.notificacoes.integration;

import com.fabriciosanches.adamodulo3.notificacoes.adapter.in.messaging.ComprovanteGeradoListener;
import com.fabriciosanches.adamodulo3.notificacoes.adapter.out.notification.ComprovanteGeradoDltPublisher;
import com.fabriciosanches.adamodulo3.notificacoes.adapter.out.notification.NotificationSender;
import com.fabriciosanches.adamodulo3.notificacoes.application.NotificationRetryPolicy;
import com.fabriciosanches.adamodulo3.notificacoes.application.ProcessNotificacaoUseCase;
import com.fabriciosanches.adamodulo3.notificacoes.application.model.ComprovanteGeradoDltRecord;
import com.fabriciosanches.adamodulo3.notificacoes.application.model.ComprovanteGeradoMessage;
import com.fabriciosanches.adamodulo3.notificacoes.config.NotificacoesObservability;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificacoesIntegrationTest {

    @Test
    void shouldRouteToDltAfterThreeFailedAttempts() {
        FailingNotificationSender sender = new FailingNotificationSender();
        CapturingDltPublisher dltPublisher = new CapturingDltPublisher();
        NotificacoesObservability observability = observability();
        ProcessNotificacaoUseCase useCase = new ProcessNotificacaoUseCase(sender, observability);
        ComprovanteGeradoListener listener = new ComprovanteGeradoListener(
                useCase,
                dltPublisher,
                observability,
                new NotificationRetryPolicy());

        Map<String, Object> payload = Map.of(
                "id", "cmp-1",
                "trace_id", "trace-1",
                "payload_pdf_json", Map.of("k", "v"));

        for (int i = 0; i < 3; i++) {
            try {
                listener.consume(payload, "trace-1");
            } catch (RuntimeException ignored) {
            }
        }

        listener.consumeDlt(payload, "trace-1", "send failed");

        assertEquals(3, sender.calls);
        assertEquals(1, dltPublisher.records.size());
        ComprovanteGeradoDltRecord dlt = dltPublisher.records.getFirst();
        assertEquals("cmp-1", dlt.id());
        assertEquals(3, dlt.attempts());
        assertEquals("send failed", dlt.error());
    }

    private static NotificacoesObservability observability() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        return new NotificacoesObservability(
                registry.counter("notificacoes_consumer_throughput_total"),
                registry.counter("notificacoes_retries_total"),
                registry.counter("notificacoes_dlt_total"));
    }

    private static final class FailingNotificationSender implements NotificationSender {
        private int calls;

        @Override
        public void send(ComprovanteGeradoMessage message) {
            calls++;
            throw new IllegalStateException("delivery failed");
        }
    }

    private static final class CapturingDltPublisher implements ComprovanteGeradoDltPublisher {
        private final List<ComprovanteGeradoDltRecord> records = new ArrayList<>();

        @Override
        public void publish(ComprovanteGeradoDltRecord record) {
            records.add(record);
        }
    }
}
