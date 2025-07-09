package com.smartdocfinder.core.controller;


import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import com.smartdocfinder.core.dto.SearchResult;
import com.smartdocfinder.core.service.LuceneService;


@RestController
@RequestMapping("/api/files")
public class DocumentController {


    @Autowired
    private LuceneService luceneService;


    

  






    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchFiles(
            @RequestParam("q") String query,
            @RequestParam(value = "maxHits", defaultValue = "100") int maxHits) {
        try {
            // The luceneService.search() method now correctly returns a Map
            Map<String, Object> response = luceneService.search(query, maxHits);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // It's good practice to log the exception here
            // logger.error("Search failed for query: {}", query, e);
            return ResponseEntity.status(500).body(Map.of("error", "Search failed due to an internal error."));
        }
    }


}