package com.fabriciosanches.adamodulo3.faturas.application.model;

import java.util.Objects;
import java.util.UUID;

public record SolicitarPagamentoCommand(UUID faturaId, String traceId, String authorizationSubject) {

    public SolicitarPagamentoCommand {
        Objects.requireNonNull(faturaId, "faturaId is required");
    }
}
