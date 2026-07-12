package com.fabriciosanches.adamodulo3.faturas.application;

import com.fabriciosanches.adamodulo3.faturas.application.model.CreateLoteCommand;
import com.fabriciosanches.adamodulo3.faturas.application.model.GetFaturaResult;
import com.fabriciosanches.adamodulo3.faturas.application.port.out.FaturaCachePort;
import com.fabriciosanches.adamodulo3.faturas.application.port.out.FaturaRepository;
import com.fabriciosanches.adamodulo3.faturas.domain.Fatura;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CreateLoteUseCase {

    private final FaturaRepository faturaRepository;
    private final FaturaCachePort cachePort;

    public CreateLoteUseCase(FaturaRepository faturaRepository, FaturaCachePort cachePort) {
        this.faturaRepository = faturaRepository;
        this.cachePort = cachePort;
    }

    public List<GetFaturaResult> execute(CreateLoteCommand command) {
        UUID loteId = command.loteId() == null ? UUID.randomUUID() : command.loteId();

        List<Fatura> pendingFaturas = new ArrayList<>();
        for (CreateLoteCommand.CreateLoteItem item : command.items()) {
            pendingFaturas.add(Fatura.createPending(UUID.randomUUID(), loteId, item.clienteDocumento(), item.valorTotal()));
        }

        List<Fatura> saved = faturaRepository.saveAll(pendingFaturas);
        List<GetFaturaResult> result = new ArrayList<>();

        for (Fatura fatura : saved) {
            GetFaturaResult snapshot = toResult(fatura);
            cachePort.putSnapshot(snapshot);
            cachePort.putStatus(fatura.getId(), fatura.getStatus(), fatura.getRetryCount());
            result.add(snapshot);
        }

        return result;
    }

    private GetFaturaResult toResult(Fatura fatura) {
        return new GetFaturaResult(
                fatura.getId(),
                fatura.getLoteId(),
                fatura.getClienteDocumento(),
                fatura.getValorTotal(),
                fatura.getStatus(),
                fatura.getRetryCount());
    }
}
