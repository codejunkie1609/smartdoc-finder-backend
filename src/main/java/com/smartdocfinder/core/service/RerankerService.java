package com.smartdocfinder.core.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdocfinder.core.dto.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class RerankerService {

    private static final Logger logger = LoggerFactory.getLogger(RerankerService.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    // ✅ CORRECTED: Manually create the constructor to use @Qualifier
    public RerankerService(@Qualifier("rerankerWebClient") WebClient webClient, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    public List<SearchResult> rerank(String query, List<SearchResult> documents) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }

        logger.info("Sending {} documents to reranker for query: '{}'", documents.size(), query);

        try {
            Map<String, Object> requestBody = Map.of(
                    "query", query,
                    "documents", documents
            );

            Map<String, Object> response = webClient.post()
                    .uri("/rerank")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("results")) {
                List<SearchResult> rerankedResults = objectMapper.convertValue(
                    response.get("results"),
                    new TypeReference<List<SearchResult>>() {}
                );
                logger.info("Successfully received {} reranked documents.", rerankedResults.size());
                return rerankedResults;
            }

        } catch (Exception e) {
            logger.error("Failed to call reranker service", e);
        }
        
        return documents;
    }
}
