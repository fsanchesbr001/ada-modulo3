package com.fabriciosanches.adamodulo3.faturas.application.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record CreateLoteCommand(UUID loteId, String traceId, List<CreateLoteItem> items) {

    public CreateLoteCommand {
        Objects.requireNonNull(items, "items is required");
        if (items.isEmpty()) {
            throw new IllegalArgumentException("items cannot be empty");
        }
    }

    public record CreateLoteItem(String clienteDocumento, BigDecimal valorTotal) {
        public CreateLoteItem {
            Objects.requireNonNull(clienteDocumento, "clienteDocumento is required");
            Objects.requireNonNull(valorTotal, "valorTotal is required");
            if (valorTotal.signum() <= 0) {
                throw new IllegalArgumentException("valorTotal must be > 0");
            }
        }
    }
}
