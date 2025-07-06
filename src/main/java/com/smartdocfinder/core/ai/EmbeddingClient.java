package com.smartdocfinder.core.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.smartdocfinder.core.dto.MultiEmbeddingResponse;

import jakarta.annotation.PostConstruct;

@Component
public class EmbeddingClient {

    @Value("${embedding.service.url:http://embedding:8000}")
    private String baseUrl;

    private WebClient webClient;

    @PostConstruct
    public void init() {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public MultiEmbeddingResponse embedQuery(String query) {
        return webClient.post()
                .uri("/multi-embed")
                .bodyValue(new EmbedRequest(query))
                .retrieve()
                .bodyToMono(MultiEmbeddingResponse.class)
                .block();
    }

    private record EmbedRequest(String text) {}
}
