package com.smartdocfinder.core.service;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

    public List<String> search(String queryStr, int maxHits) throws Exception {
        List<String> results = new ArrayList<>();

        try (DirectoryReader reader = DirectoryReader.open(luceneDirectory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            QueryParser parser = new QueryParser("content", luceneAnalyzer);
            Query query = parser.parse(queryStr);

            TopDocs hits = searcher.search(query, maxHits);
            StoredFields storedFields = searcher.storedFields();
            for (ScoreDoc hit : hits.scoreDocs) {
                Document doc = storedFields.document(hit.doc);
                results.add(doc.get("filename"));
            }
        }

        return results;
    }
}
