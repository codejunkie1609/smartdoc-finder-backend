package com.smartdocfinder.core.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.smartdocfinder.core.ai.EmbeddingClient;
import com.smartdocfinder.core.dto.MultiEmbeddingResponse;

@Component
public class SemanticSearchService {
    @Autowired private EmbeddingClient embeddingClient;

    public void testSemanticEmbedding(){
        MultiEmbeddingResponse response = embeddingClient.embedQuery("how to download payslip");
        System.out.println("base vector length: "+response.getBase().size());
        System.out.println("Sub-Query count: "+response.getSub().size());
    }
    
}
