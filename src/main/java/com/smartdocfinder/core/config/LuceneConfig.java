package com.smartdocfinder.core.config;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.store.FSDirectory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Path;

@Configuration
public class LuceneConfig {

    private static final String LUCENE_INDEX_DIR = System.getProperty("user.home") + "/smartdoc-index";

    @Bean
    public FSDirectory luceneDirectory() throws IOException {
        Path path = Path.of(LUCENE_INDEX_DIR);
        return FSDirectory.open(path);
    }

    @Bean
    public Analyzer luceneAnalyzer() {
        return new EnglishAnalyzer();  
    }
}
