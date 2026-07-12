package com.fabriciosanches.adamodulo3.comprovantes.adapter.out.messaging;

import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

public class ComprovanteQueuePublisher {

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;

    public ComprovanteQueuePublisher(RabbitTemplate rabbitTemplate, String exchange, String routingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    public void publish(ComprovanteQueueMessage message) {
        MessagePostProcessor traceHeader = amqpMessage -> {
            amqpMessage.getMessageProperties().setHeader("trace_id", message.traceId());
            return amqpMessage;
        };
        rabbitTemplate.convertAndSend(exchange, routingKey, message, traceHeader);
    }
}
