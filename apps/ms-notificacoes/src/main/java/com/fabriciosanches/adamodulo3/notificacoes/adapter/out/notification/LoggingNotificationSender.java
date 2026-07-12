package com.fabriciosanches.adamodulo3.notificacoes.adapter.out.notification;

import com.fabriciosanches.adamodulo3.notificacoes.application.model.ComprovanteGeradoMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingNotificationSender implements NotificationSender {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingNotificationSender.class);

    @Override
    public void send(ComprovanteGeradoMessage message) {
        LOG.info(
                "{\"event_type\":\"notification_sent\",\"id\":\"{}\",\"trace_id\":\"{}\"}",
                message.id(),
                message.traceId());
    }
}
