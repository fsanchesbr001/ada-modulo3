package com.fabriciosanches.adamodulo3.faturas.application.port.out;

import com.fabriciosanches.adamodulo3.faturas.application.model.GetFaturaResult;
import com.fabriciosanches.adamodulo3.faturas.domain.FaturaStatus;
import java.util.Optional;
import java.util.UUID;

public interface FaturaCachePort {

    Optional<GetFaturaResult> getSnapshot(UUID faturaId);

    void putSnapshot(GetFaturaResult snapshot);

    void putStatus(UUID faturaId, FaturaStatus status, int retryCount);
}
