package com.fabriciosanches.adamodulo3.notificacoes.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;

@Configuration
public class ObservabilityConfig {

    @Bean
    Counter notificacoesConsumerThroughputTotal(MeterRegistry meterRegistry) {
        return Counter.builder("notificacoes_consumer_throughput_total").register(meterRegistry);
    }

    @Bean
    Counter notificacoesRetriesTotal(MeterRegistry meterRegistry) {
        return Counter.builder("notificacoes_retries_total").register(meterRegistry);
    }

    @Bean
    Counter notificacoesDltTotal(MeterRegistry meterRegistry) {
        return Counter.builder("notificacoes_dlt_total").register(meterRegistry);
    }

    @Bean
    NotificacoesObservability notificacoesObservability(
            @Qualifier("notificacoesConsumerThroughputTotal") Counter notificacoesConsumerThroughputTotal,
            @Qualifier("notificacoesRetriesTotal") Counter notificacoesRetriesTotal,
            @Qualifier("notificacoesDltTotal") Counter notificacoesDltTotal) {
        return new NotificacoesObservability(
                notificacoesConsumerThroughputTotal,
                notificacoesRetriesTotal,
                notificacoesDltTotal);
    }
}
