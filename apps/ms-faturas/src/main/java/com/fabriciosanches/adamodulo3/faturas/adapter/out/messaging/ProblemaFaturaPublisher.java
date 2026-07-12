package com.fabriciosanches.adamodulo3.faturas.adapter.out.messaging;

import com.fabriciosanches.adamodulo3.faturas.application.port.out.ProblemaFaturaPublisherPort;
import com.fabriciosanches.adamodulo3.observability.MdcEnricher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

public class ProblemaFaturaPublisher implements ProblemaFaturaPublisherPort {

    private static final Logger LOG = LoggerFactory.getLogger(ProblemaFaturaPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;
    private final Counter backofficeProblemRoutesTotal;

    public ProblemaFaturaPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper, String topic) {
        this(kafkaTemplate, objectMapper, topic, null);
    }

    public ProblemaFaturaPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            String topic,
            Counter backofficeProblemRoutesTotal) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
        this.backofficeProblemRoutesTotal = backofficeProblemRoutesTotal;
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

        if (backofficeProblemRoutesTotal != null) {
            backofficeProblemRoutesTotal.increment();
        }
        MdcEnricher.put("trace_id", traceId);
        MdcEnricher.put("fatura_id", faturaId.toString());
        LOG.info(
                "{\"event_type\":\"backoffice_problem_route_published\",\"fatura_id\":\"{}\",\"idempotency_key\":\"{}\",\"retry_count_final\":{}}",
                faturaId,
                idempotencyKey,
                retryCountFinal);
        MdcEnricher.clear();
    }
}
