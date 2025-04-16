package com.smartdocfinder.core.service;

import java.util.List;
import java.util.Map;

import com.smartdocfinder.core.model.DocumentEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.smartdocfinder.core.dto.SemanticSearchResponse;

import com.smartdocfinder.core.repository.DocumentRepository;

@Component
public class SemanticSearchService {

    private final WebClient webClient = WebClient.builder()
            .baseUrl("http://localhost:8000")
            .build();

    @Autowired
    private DocumentRepository documentRepository;

    public SemanticSearchResponse search(List<Float> baseVector) {
        return webClient.post()
                .uri("/semantic-search")
                .bodyValue(Map.of("vector", baseVector))
                .retrieve()
                .bodyToMono(SemanticSearchResponse.class)
                .block();
    }

    public DocumentEntity fetchDocumentById(Long id) {
        return documentRepository.findById(id).orElse(null);
    }

}
