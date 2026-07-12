package com.fabriciosanches.adamodulo3.notificacoes.adapter.out.notification;

import com.fabriciosanches.adamodulo3.notificacoes.application.model.ComprovanteGeradoDltRecord;
import org.springframework.kafka.core.KafkaTemplate;

public class KafkaComprovanteGeradoDltPublisher implements ComprovanteGeradoDltPublisher {

    private final KafkaTemplate<String, ComprovanteGeradoDltRecord> kafkaTemplate;
    private final String topic;

    public KafkaComprovanteGeradoDltPublisher(
            KafkaTemplate<String, ComprovanteGeradoDltRecord> kafkaTemplate,
            String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public void publish(ComprovanteGeradoDltRecord record) {
        kafkaTemplate.send(topic, record.id(), record);
    }
}
