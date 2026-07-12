package com.fabriciosanches.adamodulo3.backoffice.config;

import com.fabriciosanches.adamodulo3.backoffice.adapter.in.messaging.ProblemaFaturaConsumer;
import com.fabriciosanches.adamodulo3.backoffice.adapter.out.persistence.mysql.ProblemaFaturaJdbcRepository;
import com.fabriciosanches.adamodulo3.backoffice.application.RegisterProblemaFaturaUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class BackofficeBeansConfig {

    @Bean
    ProblemaFaturaJdbcRepository problemaFaturaJdbcRepository(JdbcTemplate jdbcTemplate) {
        return new ProblemaFaturaJdbcRepository(jdbcTemplate);
    }

    @Bean
    RegisterProblemaFaturaUseCase registerProblemaFaturaUseCase(
            ProblemaFaturaJdbcRepository repository,
            BackofficeObservability observability) {
        return new RegisterProblemaFaturaUseCase(repository, observability);
    }

    @Bean
    ProblemaFaturaConsumer problemaFaturaConsumer(
            RegisterProblemaFaturaUseCase useCase,
            ObjectMapper objectMapper) {
        return new ProblemaFaturaConsumer(useCase, objectMapper);
    }
}
