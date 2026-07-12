package com.fabriciosanches.adamodulo3.pagamentos.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObservabilityConfig {

    @Bean
    Counter pagamentosPagoBlockedTotal(MeterRegistry meterRegistry) {
        return Counter.builder("pagamentos_pago_blocked_total").register(meterRegistry);
    }

    @Bean
    Counter pagamentosCompensationsTotal(MeterRegistry meterRegistry) {
        return Counter.builder("pagamentos_compensations_total").register(meterRegistry);
    }

    @Bean
    Counter pagamentosPagarConsumedTotal(MeterRegistry meterRegistry) {
        return Counter.builder("pagamentos_pagar_consumed_total").register(meterRegistry);
    }

    @Bean
    Counter pagamentosPagoConfirmedTotal(MeterRegistry meterRegistry) {
        return Counter.builder("pagamentos_pago_confirmed_total").register(meterRegistry);
    }

    @Bean
    PagamentosObservability pagamentosObservability(
            Counter pagamentosPagoBlockedTotal,
            Counter pagamentosCompensationsTotal,
            Counter pagamentosPagarConsumedTotal,
            Counter pagamentosPagoConfirmedTotal) {
        return new PagamentosObservability(
                pagamentosPagoBlockedTotal,
                pagamentosCompensationsTotal,
                pagamentosPagarConsumedTotal,
                pagamentosPagoConfirmedTotal);
    }
}
