package com.fabriciosanches.adamodulo3.backoffice.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObservabilityConfig {

    @Bean
    Counter backofficeProblemRoutesTotal(MeterRegistry meterRegistry) {
        return Counter.builder("backoffice_problem_routes_total").register(meterRegistry);
    }

    @Bean
    BackofficeObservability backofficeObservability(Counter backofficeProblemRoutesTotal) {
        return new BackofficeObservability(backofficeProblemRoutesTotal);
    }
}
