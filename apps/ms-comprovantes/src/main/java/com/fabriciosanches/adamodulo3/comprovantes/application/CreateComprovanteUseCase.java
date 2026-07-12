package com.fabriciosanches.adamodulo3.comprovantes.application;

import com.fabriciosanches.adamodulo3.comprovantes.adapter.out.messaging.ComprovanteQueueMessage;
import com.fabriciosanches.adamodulo3.comprovantes.adapter.out.messaging.ComprovanteQueuePublisher;
import com.fabriciosanches.adamodulo3.comprovantes.application.port.out.ComprovanteCachePort;
import com.fabriciosanches.adamodulo3.comprovantes.application.port.out.ComprovanteRepository;
import com.fabriciosanches.adamodulo3.comprovantes.config.ComprovantesObservability;
import com.fabriciosanches.adamodulo3.comprovantes.domain.Comprovante;
import java.util.Map;

public class CreateComprovanteUseCase {

    private final ComprovanteRepository repository;
    private final ComprovanteCachePort cachePort;
    private final ComprovanteQueuePublisher queuePublisher;
    private final ComprovantesObservability observability;

    public CreateComprovanteUseCase(ComprovanteRepository repository, ComprovanteCachePort cachePort) {
        this(repository, cachePort, null, null);
    }

    public CreateComprovanteUseCase(
            ComprovanteRepository repository,
            ComprovanteCachePort cachePort,
            ComprovanteQueuePublisher queuePublisher,
            ComprovantesObservability observability) {
        this.repository = repository;
        this.cachePort = cachePort;
        this.queuePublisher = queuePublisher;
        this.observability = observability;
    }

    public String execute(Map<String, Object> payloadPdfJson) {
        Comprovante comprovante = Comprovante.accept(payloadPdfJson);
        Comprovante saved = repository.save(comprovante);
        cachePort.put(saved);

        if (observability != null) {
            observability.onPostAccepted(saved.getId());
        }

        if (queuePublisher != null) {
            String traceId = resolveTraceId(payloadPdfJson, saved.getId());
            queuePublisher.publish(new ComprovanteQueueMessage(saved.getId(), traceId, saved.getPayloadPdfJson()));
            if (observability != null) {
                observability.onQueuePublished(saved.getId(), traceId);
            }
        }

        return saved.getId();
    }

    private String resolveTraceId(Map<String, Object> payloadPdfJson, String fallback) {
        Object traceId = payloadPdfJson == null ? null : payloadPdfJson.get("trace_id");
        if (traceId == null) {
            return fallback;
        }
        String value = String.valueOf(traceId).trim();
        return value.isBlank() ? fallback : value;
    }
}
