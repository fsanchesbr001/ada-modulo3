package com.fabriciosanches.adamodulo3.notificacoes.adapter.in.messaging;

import com.fabriciosanches.adamodulo3.notificacoes.adapter.out.notification.ComprovanteGeradoDltPublisher;
import com.fabriciosanches.adamodulo3.notificacoes.application.NotificationRetryPolicy;
import com.fabriciosanches.adamodulo3.notificacoes.application.ProcessNotificacaoUseCase;
import com.fabriciosanches.adamodulo3.notificacoes.application.model.ComprovanteGeradoDltRecord;
import com.fabriciosanches.adamodulo3.notificacoes.config.NotificacoesObservability;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.messaging.handler.annotation.Header;

public class ComprovanteGeradoListener {

    private final ProcessNotificacaoUseCase processNotificacaoUseCase;
    private final ComprovanteGeradoDltPublisher dltPublisher;
    private final NotificacoesObservability observability;
    private final NotificationRetryPolicy retryPolicy;
    private final Map<String, Integer> attemptsById = new ConcurrentHashMap<>();

    public ComprovanteGeradoListener(
            ProcessNotificacaoUseCase processNotificacaoUseCase,
            ComprovanteGeradoDltPublisher dltPublisher,
            NotificacoesObservability observability,
            NotificationRetryPolicy retryPolicy) {
        this.processNotificacaoUseCase = processNotificacaoUseCase;
        this.dltPublisher = dltPublisher;
        this.observability = observability;
        this.retryPolicy = retryPolicy;
    }

    @RetryableTopic(
            attempts = "3",
            dltStrategy = DltStrategy.ALWAYS_RETRY_ON_ERROR,
            autoCreateTopics = "false")
    @KafkaListener(topics = "${messaging.kafka.topics.comprovante-gerado:comprovante.gerado.topic}")
    public void consume(
            Map<String, Object> payload,
            @Header(name = "trace_id", required = false) String traceId) {
        String id = String.valueOf(payload.get("id"));
        int attempt = attemptsById.compute(id, (key, value) -> value == null ? 1 : value + 1);
        try {
            processNotificacaoUseCase.process(payload, traceId, attempt);
            attemptsById.remove(id);
        } catch (RuntimeException ex) {
            observability.onRetry(id, resolveTraceId(payload, traceId), attempt);
            throw ex;
        }
    }

    @KafkaListener(topics = "${messaging.kafka.topics.comprovante-gerado-dlt:comprovante.gerado.DLT}")
    public void consumeDlt(
            Map<String, Object> payload,
            @Header(name = "trace_id", required = false) String traceId,
            @Header(name = "x-exception-message", required = false) String errorHeader) {
        String id = String.valueOf(payload.get("id"));
        int attempts = attemptsById.getOrDefault(id, NotificationRetryPolicy.MAX_ATTEMPTS);
        String resolvedTrace = resolveTraceId(payload, traceId);
        String error = errorHeader == null || errorHeader.isBlank() ? "notification retries exhausted" : errorHeader;

        if (!retryPolicy.hasExceededMaxAttempts(attempts)) {
            attempts = NotificationRetryPolicy.MAX_ATTEMPTS;
        }

        ComprovanteGeradoDltRecord record = new ComprovanteGeradoDltRecord(
                id,
                resolvedTrace,
                castPayload(payload.get("payload_pdf_json")),
                attempts,
                error);

        dltPublisher.publish(record);
        observability.onDlt(id, resolvedTrace, attempts, error);
        attemptsById.remove(id);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castPayload(Object payloadPdfJson) {
        return payloadPdfJson instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private String resolveTraceId(Map<String, Object> payload, String traceId) {
        if (traceId != null && !traceId.isBlank()) {
            return traceId;
        }
        Object value = payload.get("trace_id");
        return value == null ? "" : String.valueOf(value);
    }
}
