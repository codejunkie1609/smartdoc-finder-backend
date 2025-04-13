package com.smartdocfinder.core.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.el.parser.ParseException;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.store.FSDirectory;
import org.springframework.stereotype.Service;

import com.smartdocfinder.core.dto.SearchResult;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LuceneService {

    private final FSDirectory luceneDirectory;
    private final Analyzer luceneAnalyzer;
    private static final int SNIPPET_LENGTH = 200;
    private static final String FUZZY_EDIT_DISTANCE = "~1";

    public void indexDocument(Long id, String filename, String content) throws IOException {
    try (IndexWriter writer = new IndexWriter(luceneDirectory, new IndexWriterConfig(luceneAnalyzer))) {
        Document doc = new Document();
        doc.add(new StringField("id", id.toString(), Field.Store.YES));
        doc.add(new TextField("filename", filename, Field.Store.YES));
        doc.add(new TextField("filename_autocomplete", filename, Field.Store.NO)); // 🔁 EdgeNGram target
        doc.add(new TextField("content", content, Field.Store.YES));
        writer.updateDocument(new Term("id", id.toString()), doc);
    }
}

public List<SearchResult> search(String queryStr, int maxHits) throws Exception {
    String normalizedQuery = normalizeQuery(queryStr);
    try (DirectoryReader reader = DirectoryReader.open(luceneDirectory)) {
        IndexSearcher searcher = new IndexSearcher(reader);
        Query finalQuery = buildFinalQuery(normalizedQuery);
        TopDocs hits = searcher.search(finalQuery, maxHits);
        return processSearchResults(searcher, hits, finalQuery);
    }
}

private String normalizeQuery(String queryStr) {
    return queryStr.trim()
            .replaceAll("[_\\-]", " ")
            .replaceAll("[(){}\\[\\].,!?]", "")
            .toLowerCase();
}

private Query buildFinalQuery(String normalizedQuery) throws ParseException, org.apache.lucene.queryparser.classic.ParseException {
    String[] fields = {"filename", "content"};
    MultiFieldQueryParser parser = new MultiFieldQueryParser(fields, luceneAnalyzer);
    parser.setDefaultOperator(QueryParser.Operator.OR);
    parser.setAllowLeadingWildcard(true);

    BooleanQuery.Builder finalQueryBuilder = new BooleanQuery.Builder();
    Query phraseQuery = parser.parse(QueryParserBase.escape(normalizedQuery));
    finalQueryBuilder.add(phraseQuery, BooleanClause.Occur.SHOULD);

    Query prefixQuery = new PrefixQuery(new Term("filename_autocomplete", normalizedQuery));
    finalQueryBuilder.add(phraseQuery, BooleanClause.Occur.SHOULD);
    finalQueryBuilder.add(prefixQuery, BooleanClause.Occur.SHOULD);

    for (String field : fields) {
        for (String token : normalizedQuery.split("\\s+")) {
            addFuzzyAndWildcardQueries(finalQueryBuilder, field, token, parser);
        }
    }
    return finalQueryBuilder.build();
}

private void addFuzzyAndWildcardQueries(BooleanQuery.Builder builder, String field, String token, MultiFieldQueryParser parser) throws ParseException, org.apache.lucene.queryparser.classic.ParseException {
    if (token.length() > 2) {
        Query fuzzy = parser.parse(field + ":" + QueryParserBase.escape(token) + FUZZY_EDIT_DISTANCE);
        builder.add(fuzzy, BooleanClause.Occur.SHOULD);
    }
    Query wildcard = parser.parse(field + ":*" + QueryParserBase.escape(token) + "*");
    builder.add(wildcard, BooleanClause.Occur.SHOULD);
}

private List<SearchResult> processSearchResults(IndexSearcher searcher, TopDocs hits, Query finalQuery) throws Exception {
    List<SearchResult> results = new ArrayList<>();
    StoredFields storedFields = searcher.storedFields();
    String[] fields = {"filename", "content"};

    for (ScoreDoc hit : hits.scoreDocs) {
        Document doc = storedFields.document(hit.doc);
        results.add(createSearchResult(doc, finalQuery, searcher, fields));
    }
    return results;
}

private SearchResult createSearchResult(Document doc, Query finalQuery, IndexSearcher searcher, String[] fields) throws Exception{
    SearchResult result = new SearchResult();
    String filename = doc.get("filename");
    String content = doc.get("content");
    Set<String> matchFields = new HashSet<>();
    String snippet = null;

    for (String field : fields) {
        QueryScorer scorer = new QueryScorer(finalQuery, field);
        Highlighter highlighter = new Highlighter(scorer);
        TokenStream stream = luceneAnalyzer.tokenStream(field, doc.get(field));
        String bestFrag = highlighter.getBestFragment(stream, doc.get(field));
        if (bestFrag != null && !bestFrag.isEmpty()) {
            matchFields.add(field);
            if (field.equals("content") && snippet == null) {
                snippet = bestFrag;
            }
        }
    }

    if (snippet == null && content != null) {
        snippet = content.length() > SNIPPET_LENGTH ? content.substring(0, SNIPPET_LENGTH) + "..." : content;
    }

    result.setFilename(filename);
    result.setMatchType(String.join(", ", matchFields));
    result.setSnippet(snippet);

    return result;
}
    

}
