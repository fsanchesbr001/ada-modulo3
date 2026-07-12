package com.fabriciosanches.adamodulo3.faturas.application;

import com.fabriciosanches.adamodulo3.faturas.application.model.GetFaturaResult;
import com.fabriciosanches.adamodulo3.faturas.application.port.out.FaturaCachePort;
import com.fabriciosanches.adamodulo3.faturas.application.port.out.FaturaRepository;
import com.fabriciosanches.adamodulo3.faturas.config.FaturasObservability;
import com.fabriciosanches.adamodulo3.faturas.domain.Fatura;
import java.util.UUID;

public class GetFaturaUseCase {

    private final FaturaRepository faturaRepository;
    private final FaturaCachePort cachePort;
    private final FaturasObservability observability;

    public GetFaturaUseCase(FaturaRepository faturaRepository, FaturaCachePort cachePort, FaturasObservability observability) {
        this.faturaRepository = faturaRepository;
        this.cachePort = cachePort;
        this.observability = observability;
    }

    public GetFaturaResult execute(UUID faturaId) {
        return cachePort.getSnapshot(faturaId)
                .orElseGet(() -> findInMysqlAndWarmCache(faturaId));
    }

    private GetFaturaResult findInMysqlAndWarmCache(UUID faturaId) {
        observability.onCacheMiss(faturaId.toString());

        Fatura fatura = faturaRepository.findById(faturaId)
                .orElseThrow(() -> new FaturaNotFoundException(faturaId));

        GetFaturaResult snapshot = new GetFaturaResult(
                fatura.getId(),
                fatura.getLoteId(),
                fatura.getClienteDocumento(),
                fatura.getValorTotal(),
                fatura.getStatus(),
                fatura.getRetryCount());

        cachePort.putSnapshot(snapshot);
        cachePort.putStatus(fatura.getId(), fatura.getStatus(), fatura.getRetryCount());
        return snapshot;
    }
}
