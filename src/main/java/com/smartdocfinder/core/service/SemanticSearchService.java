package com.smartdocfinder.core.service;

import com.smartdocfinder.core.dto.SemanticSearchResponse;
import com.smartdocfinder.core.model.DocumentEntity;
import com.smartdocfinder.core.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector; // ✅ Import this
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient; // ✅ Import this
import java.time.Duration; // ✅ Import this
import java.util.List;
import java.util.Map;

@Service
public class SemanticSearchService {

    private final WebClient webClient;
    private final DocumentRepository documentRepository;

    public SemanticSearchService(
            DocumentRepository documentRepository,
            // I also corrected the default fallback URL to use the correct service name
            @Value("${embedding.service.url:http://embedding-api:8000}") String baseUrl) {

        System.out.println("Embedding Base URL: " + baseUrl);

        // ✅ Create an HttpClient with a longer timeout
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(60)); // Set timeout to 60 seconds

        this.documentRepository = documentRepository;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient)) // ✅ Use the configured client
                .build();
    }

    public SemanticSearchResponse search(List<Float> baseVector) {
        return webClient.post()
                .uri("/semantic-search")
                .bodyValue(Map.of("vector", baseVector))
                .retrieve()
                .bodyToMono(SemanticSearchResponse.class)
                .block(); // .block() waits for the result, now with a longer timeout
    }

    public DocumentEntity fetchDocumentById(Long id) {
        return documentRepository.findById(id).orElse(null);
    }
}