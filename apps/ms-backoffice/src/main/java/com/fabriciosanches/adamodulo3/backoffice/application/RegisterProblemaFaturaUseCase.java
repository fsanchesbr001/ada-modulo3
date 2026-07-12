package com.fabriciosanches.adamodulo3.backoffice.application;

import com.fabriciosanches.adamodulo3.backoffice.adapter.in.messaging.ProblemaFaturaRoutingEvent;
import com.fabriciosanches.adamodulo3.backoffice.adapter.out.persistence.mysql.ProblemaFaturaJdbcRepository;
import com.fabriciosanches.adamodulo3.backoffice.adapter.out.persistence.mysql.ProblemaFaturaRecord;
import com.fabriciosanches.adamodulo3.backoffice.config.BackofficeObservability;

public class RegisterProblemaFaturaUseCase {

    private final ProblemaFaturaJdbcRepository repository;
    private final BackofficeObservability observability;

    public RegisterProblemaFaturaUseCase(
            ProblemaFaturaJdbcRepository repository,
            BackofficeObservability observability) {
        this.repository = repository;
        this.observability = observability;
    }

    public void execute(ProblemaFaturaRoutingEvent event) {
        boolean inserted = repository.saveIfAbsent(new ProblemaFaturaRecord(
                event.idempotencyKey(),
                event.faturaId(),
                event.motivo(),
                event.retryCountFinal(),
                event.payloadContexto(),
                event.traceId()));
        if (inserted) {
            observability.onProblemaRouteRegistered(event);
        }
    }
}
