package com.fabriciosanches.adamodulo3.comprovantes.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObservabilityConfig {

    @Bean
    Counter comprovantesPostsAcceptedTotal(MeterRegistry meterRegistry) {
        return Counter.builder("comprovantes_posts_accepted_total").register(meterRegistry);
    }

    @Bean
    Counter comprovantesQueuePublicationsTotal(MeterRegistry meterRegistry) {
        return Counter.builder("comprovantes_queue_publications_total").register(meterRegistry);
    }

    @Bean
    Counter comprovantesConsumerFailuresTotal(MeterRegistry meterRegistry) {
        return Counter.builder("comprovantes_consumer_failures_total").register(meterRegistry);
    }

    @Bean
    Counter comprovantesCacheHitsTotal(MeterRegistry meterRegistry) {
        return Counter.builder("comprovantes_cache_hits_total").register(meterRegistry);
    }

    @Bean
    Counter comprovantesCacheMissesTotal(MeterRegistry meterRegistry) {
        return Counter.builder("comprovantes_cache_misses_total").register(meterRegistry);
    }

    @Bean
    Counter comprovantesGetRetriesTotal(MeterRegistry meterRegistry) {
        return Counter.builder("comprovantes_get_retries_total").register(meterRegistry);
    }

    @Bean
    ComprovantesObservability comprovantesObservability(
            @Qualifier("comprovantesPostsAcceptedTotal") Counter comprovantesPostsAcceptedTotal,
            @Qualifier("comprovantesQueuePublicationsTotal") Counter comprovantesQueuePublicationsTotal,
            @Qualifier("comprovantesConsumerFailuresTotal") Counter comprovantesConsumerFailuresTotal,
            @Qualifier("comprovantesCacheHitsTotal") Counter comprovantesCacheHitsTotal,
            @Qualifier("comprovantesCacheMissesTotal") Counter comprovantesCacheMissesTotal,
            @Qualifier("comprovantesGetRetriesTotal") Counter comprovantesGetRetriesTotal) {
        return new ComprovantesObservability(
                comprovantesPostsAcceptedTotal,
                comprovantesQueuePublicationsTotal,
                comprovantesConsumerFailuresTotal,
                comprovantesCacheHitsTotal,
                comprovantesCacheMissesTotal,
                comprovantesGetRetriesTotal);
    }
}
