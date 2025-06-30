package com.smartdocfinder.core.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.smartdocfinder.core.dto.BeirDocument;
import com.smartdocfinder.core.dto.SearchResult;
import com.smartdocfinder.core.service.DocumentUploadService;
import com.smartdocfinder.core.service.LuceneService;


@RestController
@RequestMapping("/api/files")
public class DocumentController {
    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);
    @Autowired
    private DocumentUploadService documentUploadService;

    @Autowired
    private LuceneService luceneService;

    @GetMapping(value = "/index-directory-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter streamIndexing(@RequestParam String path) {
    SseEmitter emitter = new SseEmitter(0L); // No timeout

    Executors.newSingleThreadExecutor().submit(() -> {
        try {
            Path targetDir = Paths.get(path).toAbsolutePath().normalize();
            if (!Files.exists(targetDir) || !Files.isDirectory(targetDir)) {
                emitter.send(SseEmitter.event().data("❌ Invalid path: " + path));
                emitter.complete();
                return;
            }

            List<Path> files = Files.walk(targetDir)
                .filter(Files::isRegularFile)
                .filter(p -> {
                    String name = p.getFileName().toString().toLowerCase();
                    int dotIndex = name.lastIndexOf('.');
                    if (dotIndex == -1) return false;
                    String ext = name.substring(dotIndex + 1);
                    return Constants.ALLOWED_EXTENSIONS.contains(ext);
                })
                .toList();

            emitter.send(SseEmitter.event().data("📁 Found " + files.size() + " files to index. Starting..."));

            int success = 0, failed = 0;

            for (int i = 0; i < files.size(); i++) {
                Path file = files.get(i);
                try (InputStream in = Files.newInputStream(file)) {
                    String contentType = Files.probeContentType(file);
                    documentUploadService.parseAndIndex(in, file.getFileName().toString(), contentType, file.toString());
                    success++;
                    emitter.send(SseEmitter.event().data("✅ Indexed: " + file.getFileName()));
                } catch (Exception e) {
                    failed++;
                    emitter.send(SseEmitter.event().data("❌ Failed: " + file.getFileName() + " (" + e.getMessage() + ")"));
                }
            }

            emitter.send(SseEmitter.event().data("🎉 Done. Success: " + success + ", Failed: " + failed));
        } catch (IOException e) {
            try {
                emitter.send(SseEmitter.event().data("🔥 Error: " + e.getMessage()));
            } catch (IOException ignored) {}
        } 
    });

    return emitter;
}


    @GetMapping("/search")
    public ResponseEntity<List<SearchResult>> search(@RequestParam("q") String query, @RequestParam("maxHits") Integer maxHits ) {
        try {
            List<SearchResult> results = luceneService.search(query, maxHits);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/index-beir")
public ResponseEntity<?> indexBeirDocument(@RequestBody BeirDocument doc) {
    try {
        // Safe BEIR directory → project-relative
        String baseDir = System.getProperty("user.dir") + "/beir/";
        File beirDir = new File(baseDir);
        
        // Create BEIR directory if it does not exist
        if (!beirDir.exists()) {
            boolean created = beirDir.mkdirs();
            if (!created) {
                throw new IOException("Failed to create BEIR directory: " + baseDir);
            }
        }

        // Simulate a "file" → save content to a temp file so FAISS can parse it later
        String filePath = baseDir + doc.getId() + ".txt";
        Files.write(Paths.get(filePath), doc.getContent().getBytes(StandardCharsets.UTF_8));

        // Call parseAndIndex → will store in DB + index in Lucene
        documentUploadService.parseAndIndex(
                new FileInputStream(filePath),
                doc.getId() + ".txt",
                "text/plain",  // BEIR docs → plain text
                filePath
        );
 
        return ResponseEntity.ok().build();

    } catch (Exception e) {
        e.printStackTrace(); // Optional: print full stack trace to console
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                             .body("Error indexing BEIR document: " + e.getMessage());
    }
}


}