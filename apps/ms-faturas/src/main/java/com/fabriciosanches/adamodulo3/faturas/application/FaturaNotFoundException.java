package com.fabriciosanches.adamodulo3.faturas.application;

import java.util.UUID;

public class FaturaNotFoundException extends RuntimeException {

    public FaturaNotFoundException(UUID faturaId) {
        super("Fatura not found: " + faturaId);
    }
}
