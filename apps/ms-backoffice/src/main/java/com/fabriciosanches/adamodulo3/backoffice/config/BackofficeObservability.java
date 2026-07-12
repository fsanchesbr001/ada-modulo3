package com.fabriciosanches.adamodulo3.backoffice.config;

import com.fabriciosanches.adamodulo3.backoffice.adapter.in.messaging.ProblemaFaturaRoutingEvent;
import com.fabriciosanches.adamodulo3.observability.MdcEnricher;
import io.micrometer.core.instrument.Counter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackofficeObservability {

    private static final Logger LOG = LoggerFactory.getLogger(BackofficeObservability.class);

    private final Counter backofficeProblemRoutesTotal;

    public BackofficeObservability(Counter backofficeProblemRoutesTotal) {
        this.backofficeProblemRoutesTotal = backofficeProblemRoutesTotal;
    }

    public void onProblemaRouteRegistered(ProblemaFaturaRoutingEvent event) {
        backofficeProblemRoutesTotal.increment();
        MdcEnricher.put("trace_id", event.traceId());
        MdcEnricher.put("fatura_id", event.faturaId());
        LOG.info(
                "{\"event_type\":\"backoffice_problem_route_registered\",\"fatura_id\":\"{}\",\"idempotency_key\":\"{}\",\"retry_count_final\":{}}",
                event.faturaId(),
                event.idempotencyKey(),
                event.retryCountFinal());
        MdcEnricher.clear();
    }
}
