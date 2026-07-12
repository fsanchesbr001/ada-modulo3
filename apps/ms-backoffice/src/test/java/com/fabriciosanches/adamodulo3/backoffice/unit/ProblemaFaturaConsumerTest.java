package com.fabriciosanches.adamodulo3.backoffice.unit;

import com.fabriciosanches.adamodulo3.backoffice.adapter.in.messaging.ProblemaFaturaConsumer;
import com.fabriciosanches.adamodulo3.backoffice.adapter.in.messaging.ProblemaFaturaRoutingEvent;
import com.fabriciosanches.adamodulo3.backoffice.application.RegisterProblemaFaturaUseCase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ProblemaFaturaConsumerTest {

    @Test
    void consumeMustDeserializePayloadAndDelegateToUseCase() {
        RegisterProblemaFaturaUseCase useCase = mock(RegisterProblemaFaturaUseCase.class);
        ProblemaFaturaConsumer consumer = new ProblemaFaturaConsumer(useCase, new ObjectMapper());

        String payload = jsonPayload(new ProblemaFaturaRoutingEvent(
                "event-1",
                "fatura-123:retry-exhausted:3",
                "trace-abc",
                "fatura-123",
                3,
                "Retry ceiling exhausted",
                "{\"source\":\"scheduler\"}",
                "2026-01-01T10:00:00Z"));

        consumer.consume(payload);

        ArgumentCaptor<ProblemaFaturaRoutingEvent> captor = ArgumentCaptor.forClass(ProblemaFaturaRoutingEvent.class);
        verify(useCase).execute(captor.capture());
        assertEquals("fatura-123:retry-exhausted:3", captor.getValue().idempotencyKey());
    }

    @Test
    void consumeMustWrapDeserializationErrors() {
        RegisterProblemaFaturaUseCase useCase = mock(RegisterProblemaFaturaUseCase.class);
        ProblemaFaturaConsumer consumer = new ProblemaFaturaConsumer(useCase, new ObjectMapper());

        assertThrows(IllegalStateException.class, () -> consumer.consume("not-a-json"));
        verifyNoInteractions(useCase);
    }

    private static String jsonPayload(ProblemaFaturaRoutingEvent event) {
        try {
            return new ObjectMapper().writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to build test payload", ex);
        }
    }
}
