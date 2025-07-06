package com.smartdocfinder.core.service;

import java.util.List;
import java.util.Map;

import com.smartdocfinder.core.dto.SemanticSearchResponse;
import com.smartdocfinder.core.model.DocumentEntity;
import com.smartdocfinder.core.repository.DocumentRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class SemanticSearchService {

    private final WebClient webClient;
    private final DocumentRepository documentRepository;

    // ✅ Inject base URL via config or fallback to Docker service name
    public SemanticSearchService(
            DocumentRepository documentRepository,
            @Value("${embedding.service.url:http://embedding:8000}") String baseUrl) {
        System.out.println("Embedding Base URL: " + baseUrl);

        this.documentRepository = documentRepository;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

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
