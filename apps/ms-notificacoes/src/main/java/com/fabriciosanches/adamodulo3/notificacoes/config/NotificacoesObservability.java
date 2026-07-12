package com.fabriciosanches.adamodulo3.notificacoes.config;

import com.fabriciosanches.adamodulo3.observability.MdcEnricher;
import io.micrometer.core.instrument.Counter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificacoesObservability {

    private static final Logger LOG = LoggerFactory.getLogger(NotificacoesObservability.class);

    private final Counter consumerThroughputTotal;
    private final Counter retriesTotal;
    private final Counter dltTotal;

    public NotificacoesObservability(
            Counter consumerThroughputTotal,
            Counter retriesTotal,
            Counter dltTotal) {
        this.consumerThroughputTotal = consumerThroughputTotal;
        this.retriesTotal = retriesTotal;
        this.dltTotal = dltTotal;
    }

    public void onConsumerThroughput(String id, String traceId, int attempt) {
        consumerThroughputTotal.increment();
        MdcEnricher.put("trace_id", traceId);
        MdcEnricher.put("comprovante_id", id);
        LOG.info(
                "{\"event_type\":\"notificacao_consumer_throughput\",\"id\":\"{}\",\"trace_id\":\"{}\",\"attempt\":{}}",
                id,
                traceId,
                attempt);
        MdcEnricher.clear();
    }

    public void onRetry(String id, String traceId, int attempt) {
        retriesTotal.increment();
        MdcEnricher.put("trace_id", traceId);
        MdcEnricher.put("comprovante_id", id);
        LOG.warn(
                "{\"event_type\":\"notificacao_retry\",\"id\":\"{}\",\"trace_id\":\"{}\",\"attempt\":{}}",
                id,
                traceId,
                attempt);
        MdcEnricher.clear();
    }

    public void onDlt(String id, String traceId, int attempts, String error) {
        dltTotal.increment();
        MdcEnricher.put("trace_id", traceId);
        MdcEnricher.put("comprovante_id", id);
        LOG.error(
                "{\"event_type\":\"notificacao_dlt\",\"id\":\"{}\",\"trace_id\":\"{}\",\"attempts\":{},\"error\":\"{}\"}",
                id,
                traceId,
                attempts,
                error);
        MdcEnricher.clear();
    }
}
