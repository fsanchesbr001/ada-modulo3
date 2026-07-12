package com.fabriciosanches.adamodulo3.notificacoes.application;

import com.fabriciosanches.adamodulo3.notificacoes.adapter.out.notification.NotificationSender;
import com.fabriciosanches.adamodulo3.notificacoes.application.model.ComprovanteGeradoMessage;
import com.fabriciosanches.adamodulo3.notificacoes.config.NotificacoesObservability;
import java.util.Map;

public class ProcessNotificacaoUseCase {

    private final NotificationSender notificationSender;
    private final NotificacoesObservability observability;

    public ProcessNotificacaoUseCase(
            NotificationSender notificationSender,
            NotificacoesObservability observability) {
        this.notificationSender = notificationSender;
        this.observability = observability;
    }

    public void process(Map<String, Object> payload, String traceId, int attempt) {
        ComprovanteGeradoMessage message = new ComprovanteGeradoMessage(
                String.valueOf(payload.get("id")),
                resolveTraceId(payload, traceId),
                castPayload(payload.get("payload_pdf_json")));

        observability.onConsumerThroughput(message.id(), message.traceId(), attempt);
        notificationSender.send(message);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castPayload(Object payload) {
        return payload instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private String resolveTraceId(Map<String, Object> payload, String traceId) {
        if (traceId != null && !traceId.isBlank()) {
            return traceId;
        }
        Object value = payload.get("trace_id");
        return value == null ? "" : String.valueOf(value);
    }
}
