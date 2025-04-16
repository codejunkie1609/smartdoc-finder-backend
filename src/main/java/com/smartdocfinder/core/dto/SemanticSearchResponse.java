package com.smartdocfinder.core.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SemanticSearchResponse {
    private List<SemanticHit> hits;

    @Getter @Setter
    public static class SemanticHit {
        private String docId;
        private float score;
    }
}
