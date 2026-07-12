package com.fabriciosanches.adamodulo3.pagamentos.application.port.out;

import com.fabriciosanches.adamodulo3.pagamentos.domain.Pagamento;
import java.util.Optional;

public interface PagamentoRepository {

    Pagamento save(Pagamento pagamento);

    Optional<Pagamento> findById(String id);

    Optional<Pagamento> findByFaturaId(String faturaId);
}
