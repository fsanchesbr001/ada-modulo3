package com.fabriciosanches.adamodulo3.comprovantes.application.port.out;

import com.fabriciosanches.adamodulo3.comprovantes.domain.Comprovante;
import java.util.Optional;

public interface ComprovanteRepository {

    Comprovante save(Comprovante comprovante);

    Optional<Comprovante> findById(String id);
}
