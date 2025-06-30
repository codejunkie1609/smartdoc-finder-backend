package com.smartdocfinder.core.service;

import java.io.IOException;
import java.util.*;
import org.apache.el.parser.ParseException;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.*;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.smartdocfinder.core.dto.MultiEmbeddingResponse;
import com.smartdocfinder.core.dto.SearchResult;
import com.smartdocfinder.core.dto.SemanticSearchResponse;
import com.smartdocfinder.core.model.DocumentEntity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import com.smartdocfinder.core.ai.EmbeddingClient;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LuceneService {

    private final FSDirectory luceneDirectory;
    private final Analyzer luceneAnalyzer;
    private static final int SNIPPET_LENGTH = 200;
    private static final String FUZZY_EDIT_DISTANCE = "~1";

    @Autowired
    private SemanticSearchService semanticSearchService;

    @Autowired
    private EmbeddingClient embeddingClient;

    @PersistenceContext
    private EntityManager entityManager;

    public void indexDocument(Long id, String filename, String content) throws IOException {
        try (IndexWriter writer = new IndexWriter(luceneDirectory, new IndexWriterConfig(luceneAnalyzer))) {
            Document doc = new Document();
            doc.add(new StringField("id", id.toString(), Field.Store.YES));
            doc.add(new TextField("filename", filename, Field.Store.YES));
            doc.add(new TextField("filename_autocomplete", filename, Field.Store.NO)); // EdgeNGram target
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

            // 🔁 Semantic embedding + FAISS search
            MultiEmbeddingResponse embed = embeddingClient.embedQuery(normalizedQuery);
            List<Float> baseVector = embed.getBase();
            SemanticSearchResponse faissResults = semanticSearchService.search(baseVector);

            // 🔁 Build rank maps
            Map<String, Integer> semanticRankMap = buildSemanticRankMap(faissResults, 0.3f);
            Map<String, Integer> luceneRankMap = buildLuceneRankMap(hits, searcher);

            // 🔁 RRF fusion
            int k = 60;
            Set<String> allDocIds = new HashSet<>();
            allDocIds.addAll(luceneRankMap.keySet());
            allDocIds.addAll(semanticRankMap.keySet());

            List<SearchResult> results = new ArrayList<>();
            for (String docId : allDocIds) {
                SearchResult result = buildSearchResult(
                    docId, hits, searcher, finalQuery, luceneRankMap, semanticRankMap, k
                );
                if (result != null) {
                    results.add(result);
                }
            }

            // ✅ Sort safely by hybrid score
            results.sort(Comparator.comparing(
                SearchResult::getHybridScore,
                Comparator.nullsLast(Comparator.reverseOrder())
            ));

            return results;
        }
    }

    private Map<String, Integer> buildSemanticRankMap(SemanticSearchResponse response, float threshold) {
    Map<String, Integer> rankMap = new HashMap<>();
    int rank = 1;
    for (SemanticSearchResponse.SemanticHit hit : response.getHits()) {
        if (hit.getScore() >= threshold) {
            rankMap.put(String.valueOf(hit.getDocId()), rank++);
        }
    }
    return rankMap;
}
private Map<String, Integer> buildLuceneRankMap(TopDocs hits, IndexSearcher searcher) throws IOException {
    Map<String, Integer> rankMap = new HashMap<>();
    int rank = 1;
    for (ScoreDoc hit : hits.scoreDocs) {
        Document doc = searcher.storedFields().document(hit.doc);
        String docId = doc.get("id");
        rankMap.put(docId, rank++);
    }
    return rankMap;
}

private SearchResult buildSearchResult(
    String docId,
    TopDocs hits,
    IndexSearcher searcher,
    Query finalQuery,
    Map<String, Integer> luceneRankMap,
    Map<String, Integer> semanticRankMap,
    int k
) {
    try {
        SearchResult result;
        boolean isLuceneHit = luceneRankMap.containsKey(docId);
        boolean isSemanticHit = semanticRankMap.containsKey(docId);

        if (isLuceneHit) {
                result = null;
                for (ScoreDoc hit : hits.scoreDocs) {
                    Document doc = searcher.storedFields().document(hit.doc);
                    if (doc.get("id").equals(docId)) {
                        result = createSearchResult(doc, finalQuery, searcher, new String[]{"filename", "content"});
                        break;
                    }
                }
                if (result == null) {
                    result = new SearchResult(); // fallback if no ScoreDoc matched
                    result.setFilename("Unknown Lucene match");
                }

                result.setSemanticOnly(!isSemanticHit);
                result.setMatchType(isSemanticHit ? "hybrid" : "lucene-only");
        } else {
            result = new SearchResult();
            Long numericId = Long.parseLong(docId);
            DocumentEntity docEntity = entityManager.find(DocumentEntity.class, numericId);
            if (docEntity != null) {
                result.setFilename(docEntity.getFileName());
                String content = docEntity.getContent();
                result.setSnippet(content.length() > SNIPPET_LENGTH
                        ? content.substring(0, SNIPPET_LENGTH) + "..."
                        : content);
                result.setSemanticOnly(true);
                result.setMatchType("semantic-only");
            } else {
                System.err.println("⚠️ No document found for docId: " + docId);
                return null;
            }
        }

        // ✅ Assign RRF scores
        float luceneRrf = isLuceneHit ? 1.0f / (k + luceneRankMap.get(docId)) : 0f;
        float semanticRrf = isSemanticHit ? 1.0f / (k + semanticRankMap.get(docId)) : 0f;
        float hybridScore = 0.5f * luceneRrf + 0.5f * semanticRrf;

        result.setLuceneScore(luceneRrf);
        result.setSemanticScore(semanticRrf);
        result.setHybridScore(hybridScore);

        return result;

    } catch (Exception e) {
        System.err.println("❌ Failed to build result for docId: " + docId + " → " + e.getMessage());
        return null;
    }
}






    private String normalizeQuery(String queryStr) {
        return queryStr.trim()
                .replaceAll("[_\\-]", " ")
                .replaceAll("[(){}\\[\\].,!?]", "")
                .toLowerCase();
    }

    private Query buildFinalQuery(String normalizedQuery)
        throws ParseException, org.apache.lucene.queryparser.classic.ParseException {
    
    String[] fields = { "filename", "content" };

    // ✅ Field boosts
    Map<String, Float> boosts = new HashMap<>();
    boosts.put("content", 2.0f);
    boosts.put("filename", 1.0f);

    MultiFieldQueryParser parser = new MultiFieldQueryParser(fields, luceneAnalyzer, boosts);
    parser.setDefaultOperator(QueryParser.Operator.OR);
    parser.setAllowLeadingWildcard(true);

    BooleanQuery.Builder finalQueryBuilder = new BooleanQuery.Builder();

    // Phrase query with MUST
    Query phraseQuery = parser.parse("\"" + QueryParserBase.escape(normalizedQuery) + "\"");
    finalQueryBuilder.add(phraseQuery, BooleanClause.Occur.MUST);

    // Autocomplete boost on filename
    Query prefixQuery = new PrefixQuery(new Term("filename_autocomplete", normalizedQuery));
    finalQueryBuilder.add(prefixQuery, BooleanClause.Occur.SHOULD);

    // OPTIONAL: add fuzzy/wildcard only for short queries (1-2 words)
    if (normalizedQuery.split("\\s+").length <= 2) {
        for (String field : fields) {
            for (String token : normalizedQuery.split("\\s+")) {
                addFuzzyAndWildcardQueries(finalQueryBuilder, field, token, parser);
            }
        }
    }

    return finalQueryBuilder.build();
}

    private void addFuzzyAndWildcardQueries(BooleanQuery.Builder builder, String field, String token,
            MultiFieldQueryParser parser) throws ParseException, org.apache.lucene.queryparser.classic.ParseException {
        if (token.length() > 2) {
            Query fuzzy = parser.parse(field + ":" + QueryParserBase.escape(token) + FUZZY_EDIT_DISTANCE);
            builder.add(fuzzy, BooleanClause.Occur.SHOULD);
        }
        Query wildcard = parser.parse(field + ":*" + QueryParserBase.escape(token) + "*");
        builder.add(wildcard, BooleanClause.Occur.SHOULD);
    }

    private SearchResult createSearchResult(Document doc, Query finalQuery, IndexSearcher searcher, String[] fields)
            throws Exception {
        SearchResult result = new SearchResult();
        String filename = doc.get("filename");
        String beirDocId = filename != null && filename.endsWith(".txt")
        ? filename.substring(0, filename.length() - 4)
        : filename;
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
        result.setBeirDocId(beirDocId);

        return result;
    }
}
