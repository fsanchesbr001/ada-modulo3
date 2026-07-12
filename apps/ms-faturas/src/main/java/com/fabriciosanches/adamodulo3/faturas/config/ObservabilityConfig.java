package com.fabriciosanches.adamodulo3.faturas.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObservabilityConfig {

    @Bean
    Counter faturasCacheMissesTotal(MeterRegistry meterRegistry) {
        return Counter.builder("faturas_cache_misses_total").register(meterRegistry);
    }

    @Bean
    Counter faturasRetryAttemptsTotal(MeterRegistry meterRegistry) {
        return Counter.builder("faturas_retry_attempts_total").register(meterRegistry);
    }

    @Bean
    Counter faturasProblemaTransitionsTotal(MeterRegistry meterRegistry) {
        return Counter.builder("faturas_problema_transitions_total").register(meterRegistry);
    }

    @Bean
    Counter backofficeProblemRoutesTotal(MeterRegistry meterRegistry) {
        return Counter.builder("backoffice_problem_routes_total").register(meterRegistry);
    }

    @Bean
    FaturasObservability faturasObservability(
            Counter faturasCacheMissesTotal,
            Counter faturasRetryAttemptsTotal,
            Counter faturasProblemaTransitionsTotal) {
        return new FaturasObservability(
                faturasCacheMissesTotal,
                faturasRetryAttemptsTotal,
                faturasProblemaTransitionsTotal);
    }
}
