package com.fabriciosanches.adamodulo3.faturas.application.port.out;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentRequestPublisher {

    void publishPaymentRequested(UUID faturaId, UUID loteId, BigDecimal valorTotal, String traceId, String authorizationSubject);
}
