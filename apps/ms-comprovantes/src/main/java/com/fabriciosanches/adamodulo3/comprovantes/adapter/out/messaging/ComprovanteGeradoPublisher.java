package com.fabriciosanches.adamodulo3.comprovantes.adapter.out.messaging;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

public class ComprovanteGeradoPublisher {

    private final KafkaTemplate<String, ComprovanteQueueMessage> kafkaTemplate;
    private final String topic;

    public ComprovanteGeradoPublisher(
            KafkaTemplate<String, ComprovanteQueueMessage> kafkaTemplate,
            String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(ComprovanteQueueMessage message) {
        Message<ComprovanteQueueMessage> kafkaMessage = MessageBuilder
                .withPayload(message)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader(KafkaHeaders.KEY, message.id())
                .setHeader("trace_id", message.traceId())
                .build();

        kafkaTemplate.send(kafkaMessage);
    }
}
