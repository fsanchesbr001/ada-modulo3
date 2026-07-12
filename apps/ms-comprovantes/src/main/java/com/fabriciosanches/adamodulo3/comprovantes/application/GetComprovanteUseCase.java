package com.fabriciosanches.adamodulo3.comprovantes.application;

import com.fabriciosanches.adamodulo3.comprovantes.application.port.out.ComprovanteCachePort;
import com.fabriciosanches.adamodulo3.comprovantes.application.port.out.ComprovanteRepository;
import com.fabriciosanches.adamodulo3.comprovantes.config.ComprovantesObservability;
import com.fabriciosanches.adamodulo3.comprovantes.domain.Comprovante;
import com.fabriciosanches.adamodulo3.comprovantes.domain.ComprovanteLookupPolicy;
import java.util.Optional;

public class GetComprovanteUseCase {

    private final ComprovanteRepository repository;
    private final ComprovanteCachePort cachePort;
    private final ComprovanteLookupPolicy lookupPolicy;
    private final ComprovantesObservability observability;

    public GetComprovanteUseCase(ComprovanteRepository repository, ComprovanteCachePort cachePort) {
        this(repository, cachePort, new ComprovanteLookupPolicy(), null);
    }

    public GetComprovanteUseCase(
            ComprovanteRepository repository,
            ComprovanteCachePort cachePort,
            ComprovanteLookupPolicy lookupPolicy,
            ComprovantesObservability observability) {
        this.repository = repository;
        this.cachePort = cachePort;
        this.lookupPolicy = lookupPolicy;
        this.observability = observability;
    }

    public Comprovante execute(String id) {
        Optional<Comprovante> cached = cachePort.get(id);
        if (cached.isPresent()) {
            if (observability != null) {
                observability.onCacheHit(id);
            }
            return cached.get();
        }

        if (observability != null) {
            observability.onCacheMiss(id);
        }

        int attempt = 1;
        while (true) {
            Optional<Comprovante> persisted = repository.findById(id);
            if (persisted.isPresent()) {
                cachePort.put(persisted.get());
                return persisted.get();
            }

            if (!lookupPolicy.shouldRetry(attempt)) {
                break;
            }

            attempt++;
            if (observability != null) {
                observability.onGetRetry(id, attempt);
            }
        }

        throw new ComprovanteNotFoundException(id);
    }
}
