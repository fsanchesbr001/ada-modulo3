package com.fabriciosanches.adamodulo3.comprovantes.domain;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class Comprovante {

    private final String id;
    private final Map<String, Object> payloadPdfJson;
    private Map<String, Object> payloadNotificacaoCompleto;
    private ComprovanteStatus status;
    private final Instant createdAt;

    public Comprovante(
            String id,
            Map<String, Object> payloadPdfJson,
            Map<String, Object> payloadNotificacaoCompleto,
            ComprovanteStatus status,
            Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.payloadPdfJson = Map.copyOf(Objects.requireNonNull(payloadPdfJson, "payloadPdfJson is required"));
        this.payloadNotificacaoCompleto = payloadNotificacaoCompleto == null ? null : Map.copyOf(payloadNotificacaoCompleto);
        this.status = Objects.requireNonNull(status, "status is required");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
    }

    public static Comprovante accept(Map<String, Object> payloadPdfJson) {
        return new Comprovante(
                UUID.randomUUID().toString(),
                payloadPdfJson,
                null,
                ComprovanteStatus.ACEITO,
                Instant.now());
    }

    public void markProcessando() {
        this.status = ComprovanteStatus.PROCESSANDO;
    }

    public void markGerado(Map<String, Object> payloadNotificacaoCompleto) {
        this.status = ComprovanteStatus.GERADO;
        this.payloadNotificacaoCompleto = payloadNotificacaoCompleto == null ? null : Map.copyOf(payloadNotificacaoCompleto);
    }

    public String getId() {
        return id;
    }

    public Map<String, Object> getPayloadPdfJson() {
        return payloadPdfJson;
    }

    public Map<String, Object> getPayloadNotificacaoCompleto() {
        return payloadNotificacaoCompleto;
    }

    public ComprovanteStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
