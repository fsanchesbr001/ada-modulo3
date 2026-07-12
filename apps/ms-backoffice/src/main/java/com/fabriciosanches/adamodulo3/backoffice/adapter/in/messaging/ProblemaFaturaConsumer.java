package com.fabriciosanches.adamodulo3.backoffice.adapter.in.messaging;

import com.fabriciosanches.adamodulo3.backoffice.application.RegisterProblemaFaturaUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;

public class ProblemaFaturaConsumer {

    private final RegisterProblemaFaturaUseCase registerProblemaFaturaUseCase;
    private final ObjectMapper objectMapper;

    public ProblemaFaturaConsumer(RegisterProblemaFaturaUseCase registerProblemaFaturaUseCase, ObjectMapper objectMapper) {
        this.registerProblemaFaturaUseCase = registerProblemaFaturaUseCase;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${messaging.kafka.topics.problema-fatura:problema-fatura-routing}")
    public void consume(String payload) {
        try {
            ProblemaFaturaRoutingEvent event = objectMapper.readValue(payload, ProblemaFaturaRoutingEvent.class);
            registerProblemaFaturaUseCase.execute(event);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to consume problema fatura routing event", ex);
        }
    }
}
