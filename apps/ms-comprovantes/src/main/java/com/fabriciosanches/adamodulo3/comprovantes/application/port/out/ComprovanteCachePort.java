package com.fabriciosanches.adamodulo3.comprovantes.application.port.out;

import com.fabriciosanches.adamodulo3.comprovantes.domain.Comprovante;
import java.util.Optional;

public interface ComprovanteCachePort {

    Optional<Comprovante> get(String id);

    void put(Comprovante comprovante);
}
