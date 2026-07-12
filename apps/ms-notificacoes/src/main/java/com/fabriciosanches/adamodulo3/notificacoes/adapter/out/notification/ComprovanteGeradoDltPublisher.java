package com.fabriciosanches.adamodulo3.notificacoes.adapter.out.notification;

import com.fabriciosanches.adamodulo3.notificacoes.application.model.ComprovanteGeradoDltRecord;

public interface ComprovanteGeradoDltPublisher {

    void publish(ComprovanteGeradoDltRecord record);
}
