package com.smartdocfinder.core.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class SearchResult {
    private String filename;
    private String matchType;
    private String snippet;
    private Float semanticScore;
    private boolean semanticOnly;
    private Float luceneScore;

}