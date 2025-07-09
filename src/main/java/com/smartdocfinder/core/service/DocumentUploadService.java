package com.smartdocfinder.core.service;

// DocumentUploadService.java

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartdocfinder.core.constants.Constants;
import com.smartdocfinder.core.events.DocumentBatchSavedEvent;
import com.smartdocfinder.core.model.DocumentEntity;
import com.smartdocfinder.core.repository.DocumentRepository;
import com.smartdocfinder.core.util.Utilities;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DocumentUploadService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentUploadService.class);

    @Autowired
    private DocumentRepository repo;

   

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    // A private record to hold intermediate processing data.
    private record ProcessedFile(
        String originalFileName, String contentType, String filePath,
        String fileHash, String content
    ) {}

    @Transactional // Ensures the entire batch operation is a single transaction
    public void processDocumentBatch(List<Path> fileBatch) {
        if (fileBatch == null || fileBatch.isEmpty()) {
            return;
        }

        // STEP 1: Pre-process all files in the batch (CPU/IO-bound work).
        // This extracts content and calculates hashes for all files first.
        List<ProcessedFile> processedFiles = fileBatch.stream().map(path -> {
            try (InputStream in = Files.newInputStream(path)) {
                byte[] fileBytes = in.readAllBytes();
                String mediaType = DocumentParserService.detectMediaType(fileBytes);
                if (!Constants.ALLOWED_TYPES.contains(mediaType)) return null; // Skip unsupported

                String content = DocumentParserService.extractContent(new ByteArrayInputStream(fileBytes));
                String fileHash = Utilities.computeFileHash(fileBytes);

                return new ProcessedFile(
                    path.getFileName().toString(),
                    Files.probeContentType(path),
                    path.toString(),
                    fileHash,
                    content
                );
            } catch (Exception e) {
                logger.warn("Failed to pre-process file: {}. Skipping.", path.getFileName(), e);
                return null;
            }
        }).filter(java.util.Objects::nonNull).collect(Collectors.toList());

        if (processedFiles.isEmpty()) {
            logger.info("No supported files to process in this batch.");
            return;
        }

        // STEP 2: Batch Deduplication (One DB Call)
        // Collect all hashes and check for their existence in a single query.
        Set<String> hashesToCheck = processedFiles.stream()
                .map(ProcessedFile::fileHash)
                .collect(Collectors.toSet());
        Set<String> existingHashes = repo.findExistingHashes(hashesToCheck);

        // STEP 3: Filter out duplicates and prepare entities for saving.
        List<DocumentEntity> documentsToSave = new ArrayList<>();
        for (ProcessedFile pf : processedFiles) {
            if (!existingHashes.contains(pf.fileHash())) {
                DocumentEntity doc = new DocumentEntity();
                doc.setFileName(pf.originalFileName());
                doc.setFileType(pf.contentType());
                doc.setUploadedAt(LocalDateTime.now());
                doc.setFileHash(pf.fileHash());
                doc.setContent(pf.content());
                doc.setFilePath(pf.filePath());
                documentsToSave.add(doc);
            }
        }

        if (documentsToSave.isEmpty()) {
            logger.info("All files in this batch were already indexed.");
            return;
        }

        // STEP 4: Batch Save (One DB Call)
        // Save all new document entities in a single transaction.
        List<DocumentEntity> savedDocs = repo.saveAll(documentsToSave);

        logger.info("Successfully saved {} new documents to the database.", savedDocs.size());

        // STEP 5: ✅ Publish a single event containing all the saved documents
        // The loop is now gone from this service.
        if (!savedDocs.isEmpty()) {
            eventPublisher.publishEvent(new DocumentBatchSavedEvent(savedDocs));
        }

    }
}