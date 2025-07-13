package com.smartdocfinder.core.service;

import com.smartdocfinder.core.dto.GeneratorResponse;
import com.smartdocfinder.core.dto.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class GeneratorService {

    private static final Logger logger = LoggerFactory.getLogger(GeneratorService.class);

    private final WebClient webClient;

    // ✅ CORRECTED: Manually create the constructor to use @Qualifier
    public GeneratorService(@Qualifier("generatorWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public String generateAnswer(String query, List<SearchResult> contextDocuments) {
        if (contextDocuments == null || contextDocuments.isEmpty()) {
            return "Could not generate an answer as no relevant documents were found.";
        }

        logger.info("Sending {} documents to generator for final answer generation.", contextDocuments.size());

        try {
            Map<String, Object> requestBody = Map.of(
                    "query", query,
                    "documents", contextDocuments
            );

            GeneratorResponse response = webClient.post()
                    .uri("/generate")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(GeneratorResponse.class)
                    .block();

            if (response != null && response.getAnswer() != null && !response.getAnswer().isEmpty()) {
                logger.info("Successfully received generated answer from LLM.");
                return response.getAnswer();
            } else {
                logger.warn("Received an empty or null answer from the generator service. Response: {}", response);
                return "The model did not provide an answer based on the provided documents.";
            }

        } catch (Exception e) {
            logger.error("Failed to call generator service", e);
            return "An error occurred while generating the answer.";
        }
    }
}
