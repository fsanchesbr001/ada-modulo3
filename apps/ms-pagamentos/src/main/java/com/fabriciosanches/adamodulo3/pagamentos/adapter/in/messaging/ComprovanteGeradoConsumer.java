package com.fabriciosanches.adamodulo3.pagamentos.adapter.in.messaging;

import com.fabriciosanches.adamodulo3.pagamentos.application.ProcessPagamentoUseCase;
import java.util.Map;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;

public class ComprovanteGeradoConsumer {

    private final ProcessPagamentoUseCase processPagamentoUseCase;

    public ComprovanteGeradoConsumer(ProcessPagamentoUseCase processPagamentoUseCase) {
        this.processPagamentoUseCase = processPagamentoUseCase;
    }

    @KafkaListener(topics = "${messaging.kafka.topics.comprovante-gerado:comprovante.gerado.topic}")
    public void consume(
            Map<String, Object> payload,
            @Header(name = "trace_id", required = false) String traceId,
            @Header(name = "fatura_id", required = false) String faturaId,
            @Header(name = "authorization_subject", required = false) String authorizationSubject) {
        String resolvedFaturaId = faturaId;
        if (resolvedFaturaId == null || resolvedFaturaId.isBlank()) {
            Object fromPayload = payload.get("fatura_id");
            resolvedFaturaId = fromPayload == null ? null : String.valueOf(fromPayload);
        }

        if (resolvedFaturaId == null || resolvedFaturaId.isBlank()) {
            return;
        }

        processPagamentoUseCase.finalizePagamentoIfConfirmed(resolvedFaturaId, traceId);
    }
}
