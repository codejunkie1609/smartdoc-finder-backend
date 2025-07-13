package com.smartdocfinder.core.controller;

import com.smartdocfinder.core.dto.RAGResponse;
import com.smartdocfinder.core.service.DocumentUploadService;
import com.smartdocfinder.core.service.LuceneService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class DocumentController {

    private final LuceneService luceneService;
   
    /**
     * The main search endpoint for the RAG pipeline.
     *
     * @param query The user's search query.
     * @return A RAGResponse containing the generated answer and source documents.
     */
    @GetMapping("/search")
    public ResponseEntity<RAGResponse> search(@RequestParam("q") String query) {
        if (query == null || query.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            // ✅ CORRECTED: Call the refactored search method with only the query string.
            // It now returns a RAGResponse object directly.
            RAGResponse response = luceneService.search(query);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // It's good practice to log the exception here
            // logger.error("An error occurred during search for query: {}", query, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    
}
