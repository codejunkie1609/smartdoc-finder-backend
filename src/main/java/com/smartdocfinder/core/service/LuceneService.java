package com.smartdocfinder.core.service;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.store.FSDirectory;
import org.springframework.stereotype.Service;

import com.smartdocfinder.core.dto.SearchResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LuceneService {

    private final FSDirectory luceneDirectory;
    private final Analyzer luceneAnalyzer;

    public void indexDocument(Long id, String filename, String content) throws IOException {
        try (IndexWriter writer = new IndexWriter(luceneDirectory, new IndexWriterConfig(luceneAnalyzer))) {
            Document doc = new Document();
            doc.add(new StringField("id", id.toString(), Field.Store.YES));
            doc.add(new TextField("filename", filename, Field.Store.YES));
            doc.add(new TextField("content", content, Field.Store.YES));
            writer.updateDocument(new Term("id", id.toString()), doc);
        }
    }

    public List<SearchResult> search(String queryStr, int maxHits) throws Exception {
        List<SearchResult> results = new ArrayList<>();

        try (DirectoryReader reader = DirectoryReader.open(luceneDirectory)) {
            IndexSearcher searcher = new IndexSearcher(reader);

            String[] fields = new String[] { "filename", "content" }; 
            MultiFieldQueryParser parser = new MultiFieldQueryParser(fields, luceneAnalyzer);
           
            Query query = parser.parse(queryStr);
            TopDocs hits = searcher.search(query, maxHits);
            StoredFields storedFields = searcher.storedFields();

            for (ScoreDoc hit : hits.scoreDocs) {
                Document doc = storedFields.document(hit.doc);
                String filename = doc.get("filename");
                String content = doc.get("content");
                Set<String> matchFields = new HashSet<>();
                String snippet = null;
                for(String field: fields){
                    QueryScorer scorer = new QueryScorer(query, field);
                    Highlighter highlighter = new Highlighter(scorer);
                    TokenStream stream = luceneAnalyzer.tokenStream(field, doc.get(field));
                    String bestFrag = highlighter.getBestFragment(stream, doc.get(field));
                    if(bestFrag != null && !bestFrag.isEmpty()){
                        matchFields.add(field);
                        if(field.equals("content") && snippet == null){
                            snippet = bestFrag;
                        }
                    }
                }
                if (snippet == null && content != null) {
                    snippet = content.length() > 200 ? content.substring(0, 200) + "..." : content;
                }
    
                SearchResult result = new SearchResult();
                result.setFilename(filename);
                result.setMatchType(String.join(", ", matchFields));
                result.setSnippet(snippet);
    
                results.add(result);
            }
        }

        return results;
    }
}
