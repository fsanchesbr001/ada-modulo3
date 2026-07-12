package com.fabriciosanches.adamodulo3.backoffice.application;

import com.fabriciosanches.adamodulo3.backoffice.adapter.in.messaging.ProblemaFaturaRoutingEvent;
import com.fabriciosanches.adamodulo3.backoffice.adapter.out.persistence.mysql.ProblemaFaturaJdbcRepository;
import com.fabriciosanches.adamodulo3.backoffice.adapter.out.persistence.mysql.ProblemaFaturaRecord;

public class RegisterProblemaFaturaUseCase {

    private final ProblemaFaturaJdbcRepository repository;

    public RegisterProblemaFaturaUseCase(ProblemaFaturaJdbcRepository repository) {
        this.repository = repository;
    }

    public void execute(ProblemaFaturaRoutingEvent event) {
        repository.saveIfAbsent(new ProblemaFaturaRecord(
            event.idempotencyKey(),
                event.faturaId(),
                event.motivo(),
                event.retryCountFinal(),
                event.payloadContexto(),
                event.traceId()));
    }
}
