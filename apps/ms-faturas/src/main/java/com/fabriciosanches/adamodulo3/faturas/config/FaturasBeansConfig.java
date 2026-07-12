package com.fabriciosanches.adamodulo3.faturas.config;

import com.fabriciosanches.adamodulo3.faturas.adapter.in.scheduler.FaturaRetryScheduler;
import com.fabriciosanches.adamodulo3.faturas.adapter.out.messaging.PagarEventPublisher;
import com.fabriciosanches.adamodulo3.faturas.adapter.out.persistence.mysql.JdbcFaturaRepository;
import com.fabriciosanches.adamodulo3.faturas.adapter.out.persistence.redis.RedisFaturaCacheAdapter;
import com.fabriciosanches.adamodulo3.faturas.application.CreateLoteUseCase;
import com.fabriciosanches.adamodulo3.faturas.application.GetFaturaUseCase;
import com.fabriciosanches.adamodulo3.faturas.application.SolicitarPagamentoUseCase;
import com.fabriciosanches.adamodulo3.faturas.application.port.out.FaturaCachePort;
import com.fabriciosanches.adamodulo3.faturas.application.port.out.FaturaRepository;
import com.fabriciosanches.adamodulo3.faturas.application.port.out.PaymentRequestPublisher;
import com.fabriciosanches.adamodulo3.faturas.domain.FaturaRetryPolicy;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import redis.clients.jedis.JedisPool;

@Configuration
public class FaturasBeansConfig {

    @Bean
    JedisPool jedisPool(
            @Value("${spring.data.redis.host:localhost}") String host,
            @Value("${spring.data.redis.port:6379}") int port) {
        return new JedisPool(host, port);
    }

    @Bean
    FaturaRepository faturaRepository(DataSource dataSource) {
        return new JdbcFaturaRepository(dataSource);
    }

    @Bean
    FaturaCachePort faturaCachePort(JedisPool jedisPool) {
        return new RedisFaturaCacheAdapter(jedisPool);
    }

    @Bean
    PaymentRequestPublisher paymentRequestPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${messaging.kafka.topics.pagar:pagar}") String pagarTopic) {
        return new PagarEventPublisher(kafkaTemplate, objectMapper, pagarTopic);
    }

    @Bean
    FaturaRetryPolicy faturaRetryPolicy() {
        return new FaturaRetryPolicy();
    }

    @Bean
    CreateLoteUseCase createLoteUseCase(FaturaRepository repository, FaturaCachePort cachePort) {
        return new CreateLoteUseCase(repository, cachePort);
    }

    @Bean
    GetFaturaUseCase getFaturaUseCase(
            FaturaRepository repository,
            FaturaCachePort cachePort,
            FaturasObservability observability) {
        return new GetFaturaUseCase(repository, cachePort, observability);
    }

    @Bean
    SolicitarPagamentoUseCase solicitarPagamentoUseCase(
            FaturaRepository repository,
            FaturaCachePort cachePort,
            PaymentRequestPublisher paymentRequestPublisher) {
        return new SolicitarPagamentoUseCase(repository, cachePort, paymentRequestPublisher);
    }

    @Bean
    FaturaRetryScheduler faturaRetryScheduler(
            FaturaRepository repository,
            FaturaCachePort cachePort,
            PaymentRequestPublisher paymentRequestPublisher,
            FaturaRetryPolicy retryPolicy,
            FaturasObservability observability) {
        return new FaturaRetryScheduler(repository, cachePort, paymentRequestPublisher, retryPolicy, observability);
    }
}
