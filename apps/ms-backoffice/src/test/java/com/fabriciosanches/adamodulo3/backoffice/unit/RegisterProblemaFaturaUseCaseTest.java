package com.fabriciosanches.adamodulo3.backoffice.unit;

import com.fabriciosanches.adamodulo3.backoffice.adapter.in.messaging.ProblemaFaturaRoutingEvent;
import com.fabriciosanches.adamodulo3.backoffice.adapter.out.persistence.mysql.ProblemaFaturaJdbcRepository;
import com.fabriciosanches.adamodulo3.backoffice.adapter.out.persistence.mysql.ProblemaFaturaRecord;
import com.fabriciosanches.adamodulo3.backoffice.application.RegisterProblemaFaturaUseCase;
import com.fabriciosanches.adamodulo3.backoffice.config.BackofficeObservability;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegisterProblemaFaturaUseCaseTest {

    @Test
    void executeMustMapRoutingEventToPersistenceRecord() {
        ProblemaFaturaJdbcRepository repository = mock(ProblemaFaturaJdbcRepository.class);
        when(repository.saveIfAbsent(org.mockito.ArgumentMatchers.any())).thenReturn(true);

        RegisterProblemaFaturaUseCase useCase = new RegisterProblemaFaturaUseCase(repository, observability());

        ProblemaFaturaRoutingEvent event = new ProblemaFaturaRoutingEvent(
                "event-1",
                "fatura-123:retry-exhausted:3",
                "trace-abc",
                "fatura-123",
                3,
                "Retry ceiling exhausted",
                "{\"source\":\"scheduler\"}",
                "2026-01-01T10:00:00Z");

        useCase.execute(event);

        ArgumentCaptor<ProblemaFaturaRecord> captor = ArgumentCaptor.forClass(ProblemaFaturaRecord.class);
        verify(repository).saveIfAbsent(captor.capture());
        ProblemaFaturaRecord persisted = captor.getValue();

        assertEquals(event.idempotencyKey(), persisted.idempotencyKey());
        assertEquals(event.faturaId(), persisted.faturaId());
        assertEquals(event.retryCountFinal(), persisted.retryCountFinal());
        assertEquals(event.traceId(), persisted.traceId());
    }

    private static BackofficeObservability observability() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        Counter routes = Counter.builder("backoffice_problem_routes_total").register(registry);
        return new BackofficeObservability(routes);
    }
}
