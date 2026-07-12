package com.fabriciosanches.adamodulo3.comprovantes.config;

import com.fabriciosanches.adamodulo3.observability.MdcEnricher;
import io.micrometer.core.instrument.Counter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComprovantesObservability {

    private static final Logger LOG = LoggerFactory.getLogger(ComprovantesObservability.class);

    private final Counter postsAcceptedTotal;
    private final Counter queuePublicationsTotal;
    private final Counter consumerFailuresTotal;
    private final Counter cacheHitsTotal;
    private final Counter cacheMissesTotal;
    private final Counter getRetriesTotal;

    public ComprovantesObservability(
            Counter postsAcceptedTotal,
            Counter queuePublicationsTotal,
            Counter consumerFailuresTotal,
            Counter cacheHitsTotal,
            Counter cacheMissesTotal,
            Counter getRetriesTotal) {
        this.postsAcceptedTotal = postsAcceptedTotal;
        this.queuePublicationsTotal = queuePublicationsTotal;
        this.consumerFailuresTotal = consumerFailuresTotal;
        this.cacheHitsTotal = cacheHitsTotal;
        this.cacheMissesTotal = cacheMissesTotal;
        this.getRetriesTotal = getRetriesTotal;
    }

    public void onPostAccepted(String comprovanteId) {
        postsAcceptedTotal.increment();
        MdcEnricher.put("comprovante_id", comprovanteId);
        LOG.info("{\"event_type\":\"comprovante_post_accepted\",\"comprovante_id\":\"{}\"}", comprovanteId);
        MdcEnricher.clear();
    }

    public void onQueuePublished(String comprovanteId, String traceId) {
        queuePublicationsTotal.increment();
        MdcEnricher.put("comprovante_id", comprovanteId);
        MdcEnricher.put("trace_id", traceId);
        LOG.info(
                "{\"event_type\":\"comprovante_queue_published\",\"comprovante_id\":\"{}\",\"trace_id\":\"{}\"}",
                comprovanteId,
                traceId);
        MdcEnricher.clear();
    }

    public void onConsumerFailure(String comprovanteId, String traceId, Exception exception) {
        consumerFailuresTotal.increment();
        MdcEnricher.put("comprovante_id", comprovanteId);
        MdcEnricher.put("trace_id", traceId);
        LOG.error(
                "{\"event_type\":\"comprovante_consumer_failure\",\"comprovante_id\":\"{}\",\"trace_id\":\"{}\",\"error\":\"{}\"}",
                comprovanteId,
                traceId,
                exception.getMessage());
        MdcEnricher.clear();
    }

    public void onCacheHit(String comprovanteId) {
        cacheHitsTotal.increment();
        MdcEnricher.put("comprovante_id", comprovanteId);
        LOG.info("{\"event_type\":\"comprovante_cache_hit\",\"comprovante_id\":\"{}\"}", comprovanteId);
        MdcEnricher.clear();
    }

    public void onCacheMiss(String comprovanteId) {
        cacheMissesTotal.increment();
        MdcEnricher.put("comprovante_id", comprovanteId);
        LOG.info("{\"event_type\":\"comprovante_cache_miss\",\"comprovante_id\":\"{}\"}", comprovanteId);
        MdcEnricher.clear();
    }

    public void onGetRetry(String comprovanteId, int attempt) {
        getRetriesTotal.increment();
        MdcEnricher.put("comprovante_id", comprovanteId);
        LOG.info(
                "{\"event_type\":\"comprovante_get_retry\",\"comprovante_id\":\"{}\",\"attempt\":{}}",
                comprovanteId,
                attempt);
        MdcEnricher.clear();
    }
}
