package com.smartdocfinder.core.ai;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.smartdocfinder.core.dto.MultiEmbeddingResponse;



@Component

public class EmbeddingClient {
    private final WebClient webClient = WebClient.builder().baseUrl("http://localhost:8000").build();

    public MultiEmbeddingResponse embedQuery(String query){
        return webClient.post().uri("/multi-embed").bodyValue(new EmbedRequest(query)).retrieve().bodyToMono(MultiEmbeddingResponse.class).block();
    }

    private record EmbedRequest(String text) {}

}
