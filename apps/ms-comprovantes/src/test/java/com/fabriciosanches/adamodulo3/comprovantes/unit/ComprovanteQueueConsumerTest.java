package com.fabriciosanches.adamodulo3.comprovantes.unit;

import com.fabriciosanches.adamodulo3.comprovantes.adapter.in.messaging.ComprovanteQueueConsumer;
import com.fabriciosanches.adamodulo3.comprovantes.adapter.out.messaging.ComprovanteGeradoPublisher;
import com.fabriciosanches.adamodulo3.comprovantes.adapter.out.messaging.ComprovanteQueueMessage;
import com.fabriciosanches.adamodulo3.comprovantes.application.port.out.ComprovanteRepository;
import com.fabriciosanches.adamodulo3.comprovantes.domain.Comprovante;
import com.fabriciosanches.adamodulo3.comprovantes.domain.ComprovanteStatus;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComprovanteQueueConsumerTest {

    @Test
    void consumerMustPersistCompletePayloadAndPublishKafkaAfterSave() {
        FakeComprovanteRepository repository = new FakeComprovanteRepository();
        FakeComprovanteGeradoPublisher publisher = new FakeComprovanteGeradoPublisher(repository);
        ComprovanteQueueConsumer consumer = new ComprovanteQueueConsumer(repository, publisher);
        Map<String, Object> pdfPayload = Map.of("k", "v");

        consumer.consume(new ComprovanteQueueMessage("1", "t-1", pdfPayload));

        assertTrue(repository.saveCalled, "Comprovante must be persisted before publication");
        assertTrue(publisher.published, "Kafka publication must happen after persistence");
        assertTrue(publisher.publishedAfterSave, "Kafka publication must happen after MySQL save");

        Comprovante persisted = repository.lastSaved;
        assertNotNull(persisted);
        assertEquals(ComprovanteStatus.GERADO, persisted.getStatus());
        assertEquals(pdfPayload, persisted.getPayloadPdfJson());

        Map<String, Object> fullPayload = persisted.getPayloadNotificacaoCompleto();
        assertNotNull(fullPayload);
        assertEquals("1", fullPayload.get("id"));
        assertEquals("t-1", fullPayload.get("trace_id"));
        assertEquals(pdfPayload, fullPayload.get("payload_pdf_json"));

        assertEquals("1", publisher.lastPublished.id());
        assertEquals("t-1", publisher.lastPublished.traceId());
    }

    private static final class FakeComprovanteRepository implements ComprovanteRepository {
        private Comprovante lastSaved;
        private boolean saveCalled;

        @Override
        public Comprovante save(Comprovante comprovante) {
            this.saveCalled = true;
            this.lastSaved = comprovante;
            return comprovante;
        }

        @Override
        public Optional<Comprovante> findById(String id) {
            return Optional.empty();
        }
    }

    private static final class FakeComprovanteGeradoPublisher extends ComprovanteGeradoPublisher {
        private final FakeComprovanteRepository repository;
        private ComprovanteQueueMessage lastPublished;
        private boolean published;
        private boolean publishedAfterSave;

        private FakeComprovanteGeradoPublisher(FakeComprovanteRepository repository) {
            super(null, "comprovante.gerado.topic");
            this.repository = repository;
        }

        @Override
        public void publish(ComprovanteQueueMessage message) {
            this.published = true;
            this.publishedAfterSave = repository.saveCalled;
            this.lastPublished = message;
        }
    }
}
