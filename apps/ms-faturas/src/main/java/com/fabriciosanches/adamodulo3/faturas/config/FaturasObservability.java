package com.fabriciosanches.adamodulo3.faturas.config;

import com.fabriciosanches.adamodulo3.faturas.domain.Fatura;
import com.fabriciosanches.adamodulo3.observability.MdcEnricher;
import io.micrometer.core.instrument.Counter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FaturasObservability {

    private static final Logger LOG = LoggerFactory.getLogger(FaturasObservability.class);

    private final Counter cacheMissesTotal;
    private final Counter retryAttemptsTotal;
    private final Counter problemaTransitionsTotal;

    public FaturasObservability(Counter cacheMissesTotal, Counter retryAttemptsTotal, Counter problemaTransitionsTotal) {
        this.cacheMissesTotal = cacheMissesTotal;
        this.retryAttemptsTotal = retryAttemptsTotal;
        this.problemaTransitionsTotal = problemaTransitionsTotal;
    }

    public void onCacheMiss(String faturaId) {
        cacheMissesTotal.increment();
        MdcEnricher.put("fatura_id", faturaId);
        LOG.info("{\"event_type\":\"fatura_cache_miss\",\"fatura_id\":\"{}\"}", faturaId);
        MdcEnricher.clear();
    }

    public void onRetryAttempt(Fatura fatura) {
        retryAttemptsTotal.increment();
        MdcEnricher.put("fatura_id", fatura.getId().toString());
        LOG.info("{\"event_type\":\"fatura_retry_attempt\",\"fatura_id\":\"{}\",\"retry_count\":{}}",
                fatura.getId(),
                fatura.getRetryCount());
        MdcEnricher.clear();
    }

    public void onProblemaTransition(Fatura fatura) {
        problemaTransitionsTotal.increment();
        MdcEnricher.put("fatura_id", fatura.getId().toString());
        LOG.warn("{\"event_type\":\"fatura_problema_transition\",\"fatura_id\":\"{}\",\"retry_count\":{}}",
                fatura.getId(),
                fatura.getRetryCount());
        MdcEnricher.clear();
    }
}
