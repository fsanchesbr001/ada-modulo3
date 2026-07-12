package com.fabriciosanches.adamodulo3.comprovantes.config;

import com.fabriciosanches.adamodulo3.comprovantes.adapter.in.messaging.ComprovanteQueueConsumer;
import com.fabriciosanches.adamodulo3.comprovantes.adapter.out.messaging.ComprovanteGeradoPublisher;
import com.fabriciosanches.adamodulo3.comprovantes.adapter.out.messaging.ComprovanteQueueMessage;
import com.fabriciosanches.adamodulo3.comprovantes.adapter.out.messaging.ComprovanteQueuePublisher;
import com.fabriciosanches.adamodulo3.comprovantes.adapter.out.persistence.mysql.JdbcComprovanteRepository;
import com.fabriciosanches.adamodulo3.comprovantes.adapter.out.persistence.redis.RedisComprovanteCacheAdapter;
import com.fabriciosanches.adamodulo3.comprovantes.application.CreateComprovanteUseCase;
import com.fabriciosanches.adamodulo3.comprovantes.application.GetComprovanteUseCase;
import com.fabriciosanches.adamodulo3.comprovantes.application.port.out.ComprovanteCachePort;
import com.fabriciosanches.adamodulo3.comprovantes.application.port.out.ComprovanteRepository;
import com.fabriciosanches.adamodulo3.comprovantes.domain.ComprovanteLookupPolicy;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import redis.clients.jedis.JedisPool;

@Configuration
public class ComprovantesBeansConfig {

    @Bean
    JedisPool jedisPool(
            @Value("${spring.data.redis.host:localhost}") String host,
            @Value("${spring.data.redis.port:6379}") int port) {
        return new JedisPool(host, port);
    }

    @Bean
    ComprovanteRepository comprovanteRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        return new JdbcComprovanteRepository(jdbcTemplate, objectMapper);
    }

    @Bean
    ComprovanteCachePort comprovanteCachePort(JedisPool jedisPool, ObjectMapper objectMapper) {
        return new RedisComprovanteCacheAdapter(jedisPool, objectMapper);
    }

    @Bean
    DirectExchange comprovanteExchange(
            @Value("${messaging.rabbitmq.exchanges.comprovante:comprovante.exchange}") String exchangeName) {
        return new DirectExchange(exchangeName, true, false);
    }

    @Bean
    Queue comprovanteQueue(
            @Value("${messaging.rabbitmq.queues.comprovante:comprovante.queue}") String queueName) {
        return new Queue(queueName, true);
    }

    @Bean
    Binding comprovanteBinding(
            Queue comprovanteQueue,
            DirectExchange comprovanteExchange,
            @Value("${messaging.rabbitmq.routing-keys.comprovante:comprovante.routing-key}") String routingKey) {
        return BindingBuilder.bind(comprovanteQueue).to(comprovanteExchange).with(routingKey);
    }

    @Bean
    ComprovanteQueuePublisher comprovanteQueuePublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${messaging.rabbitmq.exchanges.comprovante:comprovante.exchange}") String exchange,
            @Value("${messaging.rabbitmq.routing-keys.comprovante:comprovante.routing-key}") String routingKey) {
        return new ComprovanteQueuePublisher(rabbitTemplate, exchange, routingKey);
    }

    @Bean
    ComprovanteGeradoPublisher comprovanteGeradoPublisher(
            KafkaTemplate<String, ComprovanteQueueMessage> kafkaTemplate,
            @Value("${messaging.kafka.topics.comprovante-gerado:comprovante.gerado.topic}") String topic) {
        return new ComprovanteGeradoPublisher(kafkaTemplate, topic);
    }

    @Bean
    CreateComprovanteUseCase createComprovanteUseCase(
            ComprovanteRepository repository,
            ComprovanteCachePort cachePort,
            ComprovanteQueuePublisher queuePublisher,
            ComprovantesObservability observability) {
        return new CreateComprovanteUseCase(repository, cachePort, queuePublisher, observability);
    }

    @Bean
    GetComprovanteUseCase getComprovanteUseCase(
            ComprovanteRepository repository,
            ComprovanteCachePort cachePort,
            ComprovantesObservability observability) {
        return new GetComprovanteUseCase(repository, cachePort, new ComprovanteLookupPolicy(), observability);
    }

    @Bean
    ComprovanteQueueConsumer comprovanteQueueConsumer(
            ComprovanteRepository repository,
            ComprovanteGeradoPublisher comprovanteGeradoPublisher,
            ComprovantesObservability observability) {
        return new ComprovanteQueueConsumer(repository, comprovanteGeradoPublisher, observability);
    }
}
