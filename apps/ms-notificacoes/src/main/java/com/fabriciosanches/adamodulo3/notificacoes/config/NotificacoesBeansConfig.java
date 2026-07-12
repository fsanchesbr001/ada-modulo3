package com.fabriciosanches.adamodulo3.notificacoes.config;

import com.fabriciosanches.adamodulo3.notificacoes.adapter.in.messaging.ComprovanteGeradoListener;
import com.fabriciosanches.adamodulo3.notificacoes.adapter.out.notification.ComprovanteGeradoDltPublisher;
import com.fabriciosanches.adamodulo3.notificacoes.adapter.out.notification.KafkaComprovanteGeradoDltPublisher;
import com.fabriciosanches.adamodulo3.notificacoes.adapter.out.notification.LoggingNotificationSender;
import com.fabriciosanches.adamodulo3.notificacoes.adapter.out.notification.NotificationSender;
import com.fabriciosanches.adamodulo3.notificacoes.application.NotificationRetryPolicy;
import com.fabriciosanches.adamodulo3.notificacoes.application.ProcessNotificacaoUseCase;
import com.fabriciosanches.adamodulo3.notificacoes.application.model.ComprovanteGeradoDltRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
public class NotificacoesBeansConfig {

    @Bean
    NotificationSender notificationSender() {
        return new LoggingNotificationSender();
    }

    @Bean
    NotificationRetryPolicy notificationRetryPolicy() {
        return new NotificationRetryPolicy();
    }

    @Bean
    ProcessNotificacaoUseCase processNotificacaoUseCase(
            NotificationSender notificationSender,
            NotificacoesObservability observability) {
        return new ProcessNotificacaoUseCase(notificationSender, observability);
    }

    @Bean
    ComprovanteGeradoDltPublisher comprovanteGeradoDltPublisher(
            KafkaTemplate<String, ComprovanteGeradoDltRecord> kafkaTemplate,
            @Value("${messaging.kafka.topics.comprovante-gerado-dlt:comprovante.gerado.DLT}") String topic) {
        return new KafkaComprovanteGeradoDltPublisher(kafkaTemplate, topic);
    }

    @Bean
    ComprovanteGeradoListener comprovanteGeradoListener(
            ProcessNotificacaoUseCase processNotificacaoUseCase,
            ComprovanteGeradoDltPublisher dltPublisher,
            NotificacoesObservability observability,
            NotificationRetryPolicy retryPolicy) {
        return new ComprovanteGeradoListener(processNotificacaoUseCase, dltPublisher, observability, retryPolicy);
    }
}
