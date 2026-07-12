package com.fabriciosanches.adamodulo3.comprovantes.application;

public class ComprovanteNotFoundException extends RuntimeException {

    public ComprovanteNotFoundException(String id) {
        super("Comprovante not found: " + id);
    }
}
