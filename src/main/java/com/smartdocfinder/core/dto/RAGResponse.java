package com.smartdocfinder.core.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents the final response from the RAG pipeline,
 * containing the generated answer and the source documents used.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RAGResponse {
    private String generatedAnswer;
    private List<SearchResult> searchResults;
}
