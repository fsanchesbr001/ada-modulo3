package com.fabriciosanches.adamodulo3.faturas.application.port.out;

import com.fabriciosanches.adamodulo3.faturas.domain.Fatura;
import com.fabriciosanches.adamodulo3.faturas.domain.FaturaStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FaturaRepository {

    Fatura save(Fatura fatura);

    List<Fatura> saveAll(List<Fatura> faturas);

    Optional<Fatura> findById(UUID faturaId);

    List<Fatura> findByStatus(FaturaStatus status);
}
