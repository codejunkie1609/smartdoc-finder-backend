package com.smartdocfinder.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.core.Queue;

@Configuration
public class RabbitMQConfig {
    public static final String QUEUE_NAME = "embedding.jobs";

    @Bean
    public Queue embeddingQueue() {
        return new Queue(QUEUE_NAME, true);
    }
}
