// Create a new file: GeneratorWarmupRunner.java
package com.smartdocfinder.core.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class GeneratorWarmupRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(GeneratorWarmupRunner.class);

    private final WebClient webClient;

    public GeneratorWarmupRunner(@Value("${generator.service.url}") String baseUrl) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("Pre-warming generator service... (This may take a few minutes)");
        try {
            webClient.get()
                .uri("/health")
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(response -> logger.info("✅ Generator service is warm and ready."))
                .doOnError(error -> logger.error("🔥 Failed to warm up generator service: {}", error.getMessage()))
                .subscribe();
        } catch (Exception e) {
            logger.error("🔥 Error initiating generator warmup: {}", e.getMessage());
        }
    }
}