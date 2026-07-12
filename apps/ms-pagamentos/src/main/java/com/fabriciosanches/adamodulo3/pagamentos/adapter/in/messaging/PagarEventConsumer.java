package com.fabriciosanches.adamodulo3.pagamentos.adapter.in.messaging;

import com.fabriciosanches.adamodulo3.pagamentos.application.ProcessPagamentoUseCase;
import com.fabriciosanches.adamodulo3.pagamentos.application.model.PagarEvent;
import java.util.Map;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;

public class PagarEventConsumer {

    private final ProcessPagamentoUseCase processPagamentoUseCase;

    public PagarEventConsumer(ProcessPagamentoUseCase processPagamentoUseCase) {
        this.processPagamentoUseCase = processPagamentoUseCase;
    }

    @KafkaListener(topics = "${messaging.kafka.topics.pagar:pagar}")
    public void consume(
            Map<String, Object> payload,
            @Header(name = "trace_id", required = false) String traceId,
            @Header(name = "authorization_subject", required = false) String authorizationSubject,
            @Header(name = "Authorization", required = false) String authorizationBearer) {
        String resolvedSubject = authorizationSubject;
        if ((resolvedSubject == null || resolvedSubject.isBlank()) && authorizationBearer != null && !authorizationBearer.isBlank()) {
            resolvedSubject = authorizationBearer;
        }

        processPagamentoUseCase.processPagarEvent(PagarEvent.fromPayload(payload, traceId, resolvedSubject));
    }
}
