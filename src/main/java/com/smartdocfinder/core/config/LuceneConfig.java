package com.smartdocfinder.core.config;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.store.FSDirectory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class LuceneConfig {

    private static final String LUCENE_INDEX_DIR = System.getProperty("user.home") + "/smartdoc-index";

    @Bean
    public FSDirectory luceneDirectory() throws IOException {
        Path path = Path.of(LUCENE_INDEX_DIR);
        return FSDirectory.open(path);
    }

    @Bean
    public Analyzer defaultAnalyzer() {
        return new EnglishAnalyzer();  
    }

    @Bean
    public Analyzer filenameAnalyzer() {
        return new StandardAnalyzer(); // used for full filename field
    }

    @Bean
    public Analyzer edgeNGramAnalyzer() throws IOException {
        return CustomAnalyzer.builder()
            .withTokenizer("standard")
            .addTokenFilter("lowercase")
            .addTokenFilter("edgeNGram", "minGramSize", "2", "maxGramSize", "20")
            .build();
    }

    @Bean
    @Primary
    public PerFieldAnalyzerWrapper luceneAnalyzer() throws IOException {
        Map<String, Analyzer> fieldAnalyzers = new HashMap<>();
        fieldAnalyzers.put("filename", filenameAnalyzer());                   // for regular Lucene filename search
        fieldAnalyzers.put("filename_autocomplete", edgeNGramAnalyzer());     // for prefix/EdgeNGram support
        return new PerFieldAnalyzerWrapper(defaultAnalyzer(), fieldAnalyzers);
    }
}
