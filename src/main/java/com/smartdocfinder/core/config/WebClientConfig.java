package com.smartdocfinder.core.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import java.time.Duration;

@Configuration
public class WebClientConfig {

    // This bean is for the Embedding API
    @Bean
    public WebClient embeddingWebClient(
            @Value("${embedding.service.url:http://embedding-api:8000}") String baseUrl) {
        
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(60));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(baseUrl)
                .build();
    }

    // ✅ ADD THIS NEW BEAN for the Reranker Service
    @Bean
    @Qualifier("rerankerWebClient")
    public WebClient rerankerWebClient(
            @Value("${reranker.service.url:http://reranker:9001}") String baseUrl) {
        
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(120)); // Give the reranker up to 2 minutes

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(baseUrl)
                .build();
    }

    @Bean
    @Qualifier("generatorWebClient")
    public WebClient generatorWebClient(
            @Value("${generator.service.url}") String baseUrl) {
        
        // Use a long timeout to account for slow model generation on CPU
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMinutes(15));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(baseUrl)
                .build();
    }
}
