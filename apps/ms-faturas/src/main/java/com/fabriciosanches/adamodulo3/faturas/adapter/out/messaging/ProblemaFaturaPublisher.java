package com.fabriciosanches.adamodulo3.faturas.adapter.out.messaging;

import com.fabriciosanches.adamodulo3.faturas.application.port.out.ProblemaFaturaPublisherPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;

public class ProblemaFaturaPublisher implements ProblemaFaturaPublisherPort {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public ProblemaFaturaPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper, String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    @Override
    public void publishProblemaFatura(
            UUID faturaId,
            int retryCountFinal,
            String idempotencyKey,
            String traceId,
            String motivo,
            String payloadContexto) {
        ProblemaFaturaRoutingPayload payload = new ProblemaFaturaRoutingPayload(
                UUID.randomUUID().toString(),
                idempotencyKey,
                traceId,
                faturaId.toString(),
                retryCountFinal,
                motivo,
                payloadContexto,
                Instant.now().toString());

        String body;
        try {
            body = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize problema fatura payload", ex);
        }

        ProducerRecord<String, String> record = new ProducerRecord<>(topic, faturaId.toString(), body);
        record.headers().add("trace_id", payload.traceId().getBytes(StandardCharsets.UTF_8));
        record.headers().add("event_id", payload.eventId().getBytes(StandardCharsets.UTF_8));
        record.headers().add("idempotency_key", payload.idempotencyKey().getBytes(StandardCharsets.UTF_8));

        kafkaTemplate.send(record);
    }
}
