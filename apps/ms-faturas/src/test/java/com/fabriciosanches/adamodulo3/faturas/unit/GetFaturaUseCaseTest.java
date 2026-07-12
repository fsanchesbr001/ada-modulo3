package com.fabriciosanches.adamodulo3.faturas.unit;

import com.fabriciosanches.adamodulo3.faturas.application.FaturaNotFoundException;
import com.fabriciosanches.adamodulo3.faturas.application.GetFaturaUseCase;
import com.fabriciosanches.adamodulo3.faturas.application.model.GetFaturaResult;
import com.fabriciosanches.adamodulo3.faturas.application.port.out.FaturaCachePort;
import com.fabriciosanches.adamodulo3.faturas.application.port.out.FaturaRepository;
import com.fabriciosanches.adamodulo3.faturas.config.FaturasObservability;
import com.fabriciosanches.adamodulo3.faturas.domain.Fatura;
import com.fabriciosanches.adamodulo3.faturas.domain.FaturaStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetFaturaUseCaseTest {

    @Test
    void executeMustReturnCacheSnapshotWithoutHittingRepository() {
        UUID faturaId = UUID.randomUUID();
        FaturaRepository repository = mock(FaturaRepository.class);
        FaturaCachePort cache = mock(FaturaCachePort.class);

        GetFaturaResult cached = new GetFaturaResult(
                faturaId,
                UUID.randomUUID(),
                "12345678901",
                new BigDecimal("100.00"),
                FaturaStatus.SOLICITADO,
                1);

        when(cache.getSnapshot(faturaId)).thenReturn(Optional.of(cached));

        GetFaturaUseCase useCase = new GetFaturaUseCase(repository, cache, observability());
        GetFaturaResult result = useCase.execute(faturaId);

        assertEquals(cached, result);
        verify(repository, never()).findById(any());
        verify(cache, never()).putSnapshot(any());
        verify(cache, never()).putStatus(any(), any(), any(Integer.class));
    }

    @Test
    void executeMustFallbackToRepositoryAndWarmCacheOnMiss() {
        UUID faturaId = UUID.randomUUID();
        UUID loteId = UUID.randomUUID();
        FaturaRepository repository = mock(FaturaRepository.class);
        FaturaCachePort cache = mock(FaturaCachePort.class);

        when(cache.getSnapshot(faturaId)).thenReturn(Optional.empty());

        Fatura persisted = new Fatura(
                faturaId,
                loteId,
                "12345678901",
                new BigDecimal("200.00"),
                "trace-1",
                FaturaStatus.PENDENTE,
                0);
        when(repository.findById(faturaId)).thenReturn(Optional.of(persisted));

        GetFaturaUseCase useCase = new GetFaturaUseCase(repository, cache, observability());
        GetFaturaResult result = useCase.execute(faturaId);

        assertEquals(faturaId, result.id());
        assertEquals(FaturaStatus.PENDENTE, result.status());

        ArgumentCaptor<GetFaturaResult> snapshotCaptor = ArgumentCaptor.forClass(GetFaturaResult.class);
        verify(cache).putSnapshot(snapshotCaptor.capture());
        verify(cache).putStatus(faturaId, FaturaStatus.PENDENTE, 0);
        assertEquals(faturaId, snapshotCaptor.getValue().id());
    }

    @Test
    void executeMustThrowWhenFaturaDoesNotExistInRepository() {
        UUID faturaId = UUID.randomUUID();
        FaturaRepository repository = mock(FaturaRepository.class);
        FaturaCachePort cache = mock(FaturaCachePort.class);

        when(cache.getSnapshot(faturaId)).thenReturn(Optional.empty());
        when(repository.findById(faturaId)).thenReturn(Optional.empty());

        GetFaturaUseCase useCase = new GetFaturaUseCase(repository, cache, observability());

        assertThrows(FaturaNotFoundException.class, () -> useCase.execute(faturaId));
        verify(cache, never()).putSnapshot(any());
    }

    private static FaturasObservability observability() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        Counter cacheMiss = Counter.builder("faturas_cache_misses_total").register(registry);
        Counter retry = Counter.builder("faturas_retry_attempts_total").register(registry);
        Counter problema = Counter.builder("faturas_problema_transitions_total").register(registry);
        return new FaturasObservability(cacheMiss, retry, problema);
    }
}
