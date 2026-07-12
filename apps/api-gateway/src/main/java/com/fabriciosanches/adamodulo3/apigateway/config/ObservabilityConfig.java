package com.fabriciosanches.adamodulo3.apigateway.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObservabilityConfig {

    @Bean
    Counter authLoginRequestsTotal(MeterRegistry meterRegistry) {
        return Counter.builder("auth_login_requests_total").register(meterRegistry);
    }

    @Bean
    Counter gatewayDownstreamPropagationFailuresTotal(MeterRegistry meterRegistry) {
        return Counter.builder("gateway_downstream_propagation_failures_total").register(meterRegistry);
    }

    @Bean
    Counter tracePropagationFailuresTotal(MeterRegistry meterRegistry) {
        return Counter.builder("trace_propagation_failures_total").register(meterRegistry);
    }
}
