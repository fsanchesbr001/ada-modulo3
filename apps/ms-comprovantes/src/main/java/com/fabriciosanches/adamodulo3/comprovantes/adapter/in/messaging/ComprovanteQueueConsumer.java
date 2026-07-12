package com.fabriciosanches.adamodulo3.comprovantes.adapter.in.messaging;

import com.fabriciosanches.adamodulo3.comprovantes.adapter.out.messaging.ComprovanteQueueMessage;
import com.fabriciosanches.adamodulo3.comprovantes.adapter.out.messaging.ComprovanteGeradoPublisher;
import com.fabriciosanches.adamodulo3.comprovantes.application.port.out.ComprovanteRepository;
import com.fabriciosanches.adamodulo3.comprovantes.config.ComprovantesObservability;
import com.fabriciosanches.adamodulo3.comprovantes.domain.Comprovante;
import com.fabriciosanches.adamodulo3.comprovantes.domain.ComprovanteStatus;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

public class ComprovanteQueueConsumer {

    private final ComprovanteRepository repository;
    private final ComprovanteGeradoPublisher comprovanteGeradoPublisher;
    private final ComprovantesObservability observability;

    public ComprovanteQueueConsumer(
            ComprovanteRepository repository,
            ComprovanteGeradoPublisher comprovanteGeradoPublisher) {
        this(repository, comprovanteGeradoPublisher, null);
    }

    public ComprovanteQueueConsumer(
            ComprovanteRepository repository,
            ComprovanteGeradoPublisher comprovanteGeradoPublisher,
            ComprovantesObservability observability) {
        this.repository = repository;
        this.comprovanteGeradoPublisher = comprovanteGeradoPublisher;
        this.observability = observability;
    }

    @RabbitListener(queues = "${messaging.rabbitmq.queues.comprovante:comprovante.queue}")
    public void consume(ComprovanteQueueMessage message) {
        try {
            Comprovante comprovanteGerado = new Comprovante(
                    message.id(),
                    message.payloadPdfJson(),
                    payloadNotificacaoCompleto(message),
                    ComprovanteStatus.GERADO,
                    Instant.now());
            repository.save(comprovanteGerado);
            comprovanteGeradoPublisher.publish(message);
        } catch (RuntimeException ex) {
            if (observability != null) {
                observability.onConsumerFailure(message.id(), message.traceId(), ex);
            }
            throw ex;
        }
    }

    private Map<String, Object> payloadNotificacaoCompleto(ComprovanteQueueMessage message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", message.id());
        payload.put("trace_id", message.traceId());
        payload.put("payload_pdf_json", message.payloadPdfJson());
        return payload;
    }
}
