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
    private Float hybridScore;
    private String id; // This is the Lucene document ID, not the database ID
    private String fileHash; // The hash of the file content
    private String contentType; // The MIME type of the file
    private String originalFileName; // The original file name as stored in the database
    private String filePath; // The path to the file in the filesystem
}