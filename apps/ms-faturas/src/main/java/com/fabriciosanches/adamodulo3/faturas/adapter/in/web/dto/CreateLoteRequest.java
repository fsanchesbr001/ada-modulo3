package com.fabriciosanches.adamodulo3.faturas.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateLoteRequest(
        UUID loteId,
        @NotEmpty List<@Valid Item> items) {

    public record Item(
            @NotBlank String clienteDocumento,
            @NotNull @Positive BigDecimal valorTotal) {
    }
}
