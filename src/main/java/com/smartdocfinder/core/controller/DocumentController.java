package com.smartdocfinder.core.controller;


import java.util.List;

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
    public ResponseEntity<List<SearchResult>> search(@RequestParam("q") String query, @RequestParam("maxHits") Integer maxHits ) {
        try {
            List<SearchResult> results = luceneService.search(query, maxHits);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


}