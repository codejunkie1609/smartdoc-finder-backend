package com.smartdocfinder.core.service;

import com.smartdocfinder.core.ai.EmbeddingClient;
import com.smartdocfinder.core.dto.MultiEmbeddingResponse;
import com.smartdocfinder.core.dto.RAGResponse;
import com.smartdocfinder.core.dto.SearchResult;
import com.smartdocfinder.core.dto.SemanticSearchResponse;
import com.smartdocfinder.core.model.DocumentEntity;
import com.smartdocfinder.core.repository.DocumentRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.store.Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor // ✅ Best Practice: Use constructor injection for required dependencies.
public class LuceneService {

    private static final Logger logger = LoggerFactory.getLogger(LuceneService.class);
    private static final int SNIPPET_LENGTH = 200;
    private static final int RRF_K = 60; // Reciprocal Rank Fusion 'k' constant

    // ✅ Dependencies are final and injected via the constructor by Lombok
    private final Directory luceneDirectory;
    private final Analyzer luceneAnalyzer;
    private final SemanticSearchService semanticSearchService;
    private final EmbeddingClient embeddingClient;
    private final DocumentRepository documentRepository; // Inject repository for batch fetching
    private final RerankerService rerankerService;
    private static final int INITIAL_RETRIEVAL_SIZE = 50; // Retrieve 50 candidates
    private static final int RERANKED_TOP_K = 5;   
    private final GeneratorService generatorService;

    private IndexWriter indexWriter;

    /**
     * ✅ CORRECT LIFECYCLE: Creates a single, long-lived IndexWriter instance.
     * This is crucial for performance and correctness.
     */
    @PostConstruct
    private void init() throws IOException {
        IndexWriterConfig iwc = new IndexWriterConfig(luceneAnalyzer);
        iwc.setRAMBufferSizeMB(256.0);
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        this.indexWriter = new IndexWriter(luceneDirectory, iwc);
        logger.info("Initialized shared IndexWriter with {}MB RAM buffer.", iwc.getRAMBufferSizeMB());
    }

    /**
     * ✅ REFACTORED: Uses the shared IndexWriter instance.
     * This is much more efficient than creating a new writer for every document.
     */
    public void indexDocument(Long id, String filename, String content) throws IOException {
        Document doc = new Document();
        doc.add(new StringField("id", id.toString(), Field.Store.YES));
        doc.add(new TextField("filename", filename, Field.Store.YES));
        doc.add(new TextField("filename_autocomplete", filename, Field.Store.NO));
        doc.add(new TextField("content", content, Field.Store.YES));
        // Use the shared writer to update the document.
        this.indexWriter.updateDocument(new Term("id", id.toString()), doc);
    }

    /**
     * ✅ REFACTORED: Main search logic is now a high-level coordinator.
     * The complex steps of fetching, ranking, and merging are delegated to private methods.
     */
    public RAGResponse search(String queryStr) throws Exception {
        // 1. Initial Retrieval (Hybrid Search)
        logger.info("--- Stage 1: Initial Retrieval ---");
        List<SearchResult> initialCandidates = retrieveInitialCandidates(queryStr);

        if (initialCandidates.isEmpty()) {
            logger.warn("No initial candidates found for query: '{}'", queryStr);
            return new RAGResponse("I could not find any relevant documents to answer your question.", List.of());
        }

        // 2. Re-ranking
        logger.info("--- Stage 2: Re-ranking Top {} Candidates ---", initialCandidates.size());
        List<SearchResult> rerankedCandidates = rerankerService.rerank(queryStr, initialCandidates);

        // 3. Generation
        logger.info("--- Stage 3: Generating Answer from Top {} Documents ---", RERANKED_TOP_K);
        List<SearchResult> finalContext = rerankedCandidates.stream().limit(RERANKED_TOP_K).collect(Collectors.toList());
        String generatedAnswer = generatorService.generateAnswer(queryStr, finalContext);

        return new RAGResponse(generatedAnswer, finalContext);
    }

    /**
     * ✅ NEW: This method performs the initial hybrid search to get a large set of candidates.
     */
    private List<SearchResult> retrieveInitialCandidates(String queryStr) throws Exception {
        try (DirectoryReader reader = DirectoryReader.open(indexWriter)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            String normalizedQuery = normalizeQuery(queryStr);
            Query luceneQuery = buildLuceneQuery(normalizedQuery);
            TopDocs luceneHits = searcher.search(luceneQuery, INITIAL_RETRIEVAL_SIZE);

            Map<String, Integer> luceneRankMap = createRankMap(luceneHits, searcher);
            Map<String, Integer> semanticRankMap = getSemanticRankings(normalizedQuery, 0.3f);

            Set<String> allDocIds = new HashSet<>(luceneRankMap.keySet());
            allDocIds.addAll(semanticRankMap.keySet());

            Map<String, Document> luceneDocuments = fetchLuceneDocuments(searcher, luceneHits, allDocIds);
            Map<Long, DocumentEntity> dbEntities = fetchDatabaseEntities(allDocIds, luceneRankMap.keySet());

            return allDocIds.stream()
                .map(docId -> buildFusedResult(
                        docId, luceneQuery, searcher,
                        luceneRankMap.get(docId), semanticRankMap.get(docId),
                        luceneDocuments.get(docId), dbEntities.get(Long.parseLong(docId))
                ))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(SearchResult::getHybridScore).reversed())
                .collect(Collectors.toList());
        }
    }
    /**
     * ✅ NEW: This method calls the generator service to get a RAG response.
     */
    private String getGeneratedAnswer(String query, List<SearchResult> searchResults) {
        // Use the top 5 results as context for the LLM
        List<SearchResult> contextDocs = searchResults.stream().limit(5).collect(Collectors.toList());

        if (contextDocs.isEmpty()) {
            return ""; // No context, no answer
        }

        RestTemplate restTemplate = new RestTemplate();
        String generatorUrl = "http://generator:8000/generate";
        
        // Create the request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("query", query);
        requestBody.put("documents", contextDocs);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            Map<String, String> response = restTemplate.postForObject(generatorUrl, requestEntity, Map.class);
            return (response != null && response.containsKey("answer")) ? response.get("answer") : "";
        } catch (Exception e) {
            logger.error("Failed to get response from generator service", e);
            return "Error: Could not generate an answer at this time.";
        }
    }
    
    
    // --- Helper Methods for Search Logic ---

    private Map<String, Integer> getSemanticRankings(String query, float threshold) throws Exception {
        MultiEmbeddingResponse embed = embeddingClient.embedQuery(query);
        SemanticSearchResponse faissResults = semanticSearchService.search(embed.getBase());
        
        Map<String, Integer> rankMap = new HashMap<>();
        int rank = 1;
        for (var hit : faissResults.getHits()) {
            if (hit.getScore() >= threshold) {
                rankMap.put(String.valueOf(hit.getDocId()), rank++);
            }
        }
        return rankMap;
    }

    private Map<String, Integer> createRankMap(TopDocs hits, IndexSearcher searcher) throws IOException {
        Map<String, Integer> rankMap = new HashMap<>();
        for (int i = 0; i < hits.scoreDocs.length; i++) {
            Document doc = searcher.storedFields().document(hits.scoreDocs[i].doc);
            rankMap.put(doc.get("id"), i + 1); // Rank is 1-based
        }
        return rankMap;
    }
    
    /**
     * ✅ NEW & EFFICIENT: Fetches Lucene documents needed for the result set.
     */
    private Map<String, Document> fetchLuceneDocuments(IndexSearcher searcher, TopDocs hits, Set<String> allDocIds) throws IOException {
        Map<String, Document> docMap = new HashMap<>();
        for (ScoreDoc scoreDoc : hits.scoreDocs) {
            Document doc = searcher.storedFields().document(scoreDoc.doc);
            String docId = doc.get("id");
            if (allDocIds.contains(docId)) { // Only fetch if it's in the combined result set
                docMap.put(docId, doc);
            }
        }
        return docMap;
    }

    /**
     * ✅ NEW & EFFICIENT: Fetches DB entities for semantic-only results in a single batch query.
     */
    private Map<Long, DocumentEntity> fetchDatabaseEntities(Set<String> allDocIds, Set<String> luceneDocIds) {
        // Determine which IDs are ONLY in the semantic results and need fetching from the DB
        Set<Long> idsToFetch = allDocIds.stream()
            .filter(id -> !luceneDocIds.contains(id))
            .map(Long::parseLong)
            .collect(Collectors.toSet());

        if (idsToFetch.isEmpty()) {
            return Collections.emptyMap();
        }

        // Fetch all required entities in one go
        return documentRepository.findAllById(idsToFetch).stream()
            .collect(Collectors.toMap(DocumentEntity::getId, Function.identity()));
    }
    
    /**
     * ✅ NEW & REFACTORED: Builds a single search result with clear logic.
     */
    private SearchResult buildFusedResult(String docId, Query luceneQuery, IndexSearcher searcher,
                                          Integer luceneRank, Integer semanticRank,
                                          Document luceneDoc, DocumentEntity dbEntity) {
        try {
            SearchResult result = new SearchResult();
            result.setId(docId); 

            // Populate main data (filename, snippet)
            if (luceneDoc != null) {
                // Data comes from the faster Lucene index
                result.setFilename(luceneDoc.get("filename"));
                result.setSnippet(createSnippet(luceneQuery, searcher, luceneDoc));
            } else if (dbEntity != null) {
                // Fallback for semantic-only results: data from the database
                result.setFilename(dbEntity.getFileName());
                String content = dbEntity.getContent();
                result.setSnippet(content.length() > SNIPPET_LENGTH ? content.substring(0, SNIPPET_LENGTH) + "..." : content);
            } else {
                logger.warn("Could not find data source for document ID: {}", docId);
                return null;
            }

            // Calculate Reciprocal Rank Fusion (RRF) score
            float luceneRrf = (luceneRank != null) ? 1.0f / (RRF_K + luceneRank) : 0f;
            float semanticRrf = (semanticRank != null) ? 1.0f / (RRF_K + semanticRank) : 0f;
            result.setHybridScore(luceneRrf + semanticRrf); // Simple addition is common for RRF

            // Set match type for debugging/UI
            if (luceneRank != null && semanticRank != null) {
                result.setMatchType("hybrid");
            } else if (luceneRank != null) {
                result.setMatchType("keyword");
            } else {
                result.setMatchType("semantic");
            }
            return result;
        } catch (Exception e) {
            logger.error("Failed to build result for docId: {}", docId, e);
            return null;
        }
    }
    
    private String createSnippet(Query query, IndexSearcher searcher, Document doc) throws Exception {
        String content = doc.get("content");
        QueryScorer scorer = new QueryScorer(query, "content");
        Highlighter highlighter = new Highlighter(scorer);
        String bestFragment = highlighter.getBestFragment(luceneAnalyzer, "content", content);
        
        if (bestFragment != null && !bestFragment.isEmpty()) {
            return bestFragment;
        }
        // Fallback snippet
        return content.length() > SNIPPET_LENGTH ? content.substring(0, SNIPPET_LENGTH) + "..." : content;
    }

    private String normalizeQuery(String queryStr) {
        return queryStr.trim().toLowerCase();
    }
    
    private Query buildLuceneQuery(String normalizedQuery) {
        try {
            String[] fields = {"filename", "content"};
            Map<String, Float> boosts = new HashMap<>();
            boosts.put("filename", 2.0f); // Boost filename matches
            boosts.put("content", 1.0f);

            MultiFieldQueryParser parser = new MultiFieldQueryParser(fields, luceneAnalyzer, boosts);
            parser.setDefaultOperator(QueryParser.Operator.AND); // Use AND for more precise results

            return parser.parse(normalizedQuery);
        } catch (Exception e) {
            logger.error("Failed to build Lucene query for: {}", normalizedQuery, e);
            return new MatchNoDocsQuery(); // Return a query that matches nothing
        }
    }

    /**
     * ✅ CORRECT LIFECYCLE: Ensures the IndexWriter is closed properly on shutdown.
     */
    @PreDestroy
    private void cleanup() throws IOException {
        if (indexWriter != null) {
            logger.info("Closing IndexWriter...");
            indexWriter.close();
        }
    }
}