package com.fabriciosanches.adamodulo3.testcontainers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

public class IntegrationEnvironment {

    public final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");
    public final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7")).withExposedPorts(6379);
    public final RabbitMQContainer rabbit = new RabbitMQContainer(DockerImageName.parse("rabbitmq:3-management"));
    public final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka:3.7.0"));

    public void start() {
        mysql.start();
        redis.start();
        rabbit.start();
        kafka.start();
    }

    public void stop() {
        kafka.stop();
        rabbit.stop();
        redis.stop();
        mysql.stop();
    }
}
