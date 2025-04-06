package com.smartdocfinder.core.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {
    @Getter @Setter private String filename;
    @Getter @Setter private String matchType;
    @Getter @Setter private String snippet;
}