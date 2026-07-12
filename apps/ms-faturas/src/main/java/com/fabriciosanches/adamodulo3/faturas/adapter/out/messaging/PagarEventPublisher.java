package com.fabriciosanches.adamodulo3.faturas.adapter.out.messaging;

import com.fabriciosanches.adamodulo3.faturas.application.port.out.PaymentRequestPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;

public class PagarEventPublisher implements PaymentRequestPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public PagarEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper, String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    @Override
    public void publishPaymentRequested(UUID faturaId, UUID loteId, BigDecimal valorTotal, String traceId, String authorizationSubject) {
        PagarEventPayload payload = new PagarEventPayload(
                UUID.randomUUID().toString(),
                faturaId.toString(),
                loteId.toString(),
                valorTotal,
                Instant.now().toString(),
                traceId,
                authorizationSubject == null || authorizationSubject.isBlank() ? "unknown" : authorizationSubject);

        String message;
        try {
            message = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize PAGAR event payload", ex);
        }

        ProducerRecord<String, String> record = new ProducerRecord<>(topic, faturaId.toString(), message);
        record.headers().add("trace_id", payload.traceId().getBytes(StandardCharsets.UTF_8));
        record.headers().add("authorization_subject", payload.authorizationSubject().getBytes(StandardCharsets.UTF_8));
        record.headers().add("event_id", payload.eventId().getBytes(StandardCharsets.UTF_8));

        kafkaTemplate.send(record);
    }
}
