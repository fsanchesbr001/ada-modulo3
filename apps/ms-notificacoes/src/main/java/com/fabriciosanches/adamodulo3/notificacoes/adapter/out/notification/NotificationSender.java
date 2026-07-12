package com.fabriciosanches.adamodulo3.notificacoes.adapter.out.notification;

import com.fabriciosanches.adamodulo3.notificacoes.application.model.ComprovanteGeradoMessage;

public interface NotificationSender {

    void send(ComprovanteGeradoMessage message);
}
