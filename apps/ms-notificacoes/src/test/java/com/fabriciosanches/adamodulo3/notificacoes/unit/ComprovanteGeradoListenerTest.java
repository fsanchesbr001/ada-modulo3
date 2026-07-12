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

class ComprovanteGeradoListenerTest {

    @Test
    void consumeMustDispatchProcessingWithAttemptCounter() {
        CapturingSender sender = new CapturingSender();
        ProcessNotificacaoUseCase useCase = new ProcessNotificacaoUseCase(sender, observability());
        ComprovanteGeradoListener listener = new ComprovanteGeradoListener(
                useCase,
                record -> {
                },
                observability(),
                new NotificationRetryPolicy());

        listener.consume(Map.of(
                "id", "cmp-1",
                "trace_id", "trace-1",
                "payload_pdf_json", Map.of("k", "v")), "trace-1");

        assertEquals("cmp-1", sender.last.id());
        assertEquals("trace-1", sender.last.traceId());
    }

    @Test
    void consumeDltMustPublishMappedDltRecord() {
        CapturingDltPublisher dltPublisher = new CapturingDltPublisher();
        ProcessNotificacaoUseCase useCase = new ProcessNotificacaoUseCase(new CapturingSender(), observability());
        ComprovanteGeradoListener listener = new ComprovanteGeradoListener(
                useCase,
                dltPublisher,
                observability(),
                new NotificationRetryPolicy());

        listener.consumeDlt(Map.of(
                        "id", "cmp-2",
                        "trace_id", "trace-2",
                        "payload_pdf_json", Map.of("x", 1)),
                "trace-2",
                "err");

        assertEquals("cmp-2", dltPublisher.last.id());
        assertEquals("trace-2", dltPublisher.last.traceId());
        assertEquals(3, dltPublisher.last.attempts());
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

    private static final class CapturingDltPublisher implements ComprovanteGeradoDltPublisher {
        private ComprovanteGeradoDltRecord last;

        @Override
        public void publish(ComprovanteGeradoDltRecord record) {
            this.last = record;
        }
    }
}
