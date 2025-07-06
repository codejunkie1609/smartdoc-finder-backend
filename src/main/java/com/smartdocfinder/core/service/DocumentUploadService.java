package com.smartdocfinder.core.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.smartdocfinder.core.constants.Constants;
import com.smartdocfinder.core.controller.DocumentController;
import com.smartdocfinder.core.model.DocumentEntity;
import com.smartdocfinder.core.repository.DocumentRepository;
import com.smartdocfinder.core.util.Utilities;

@Service
public class DocumentUploadService {
    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);
    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private LuceneService luceneService;

   

    public void parseAndIndex(InputStream stream, String originalFileName, String contentType, String filePath)
        throws Exception {

    byte[] fileBytes = stream.readAllBytes();

    // Validate MIME type
    String mediaType = DocumentParserService.detectMediaType(fileBytes);
    logger.info("media type: {}", mediaType);
    if (!Constants.ALLOWED_TYPES.contains(mediaType)) {
        logger.info("Unsupported file type: {}", mediaType);
        return;  // ✅ Skip unsupported file
    }

    // Extract content
    String content = DocumentParserService.extractContent(new ByteArrayInputStream(fileBytes));

    // Deduplication via file hash
    String fileHash = Utilities.computeFileHash(fileBytes);
    DocumentRepository repo = applicationContext.getBean(DocumentRepository.class);

    if (repo.existsByFileHash(fileHash)) {
        logger.info("Skipping already indexed file: {}", originalFileName);
        return;  // ✅ Gracefully skip
    }

    // Save metadata
    DocumentEntity doc = new DocumentEntity();
    doc.setFileName(originalFileName);
    doc.setFileType(contentType);
    doc.setUploadedAt(LocalDateTime.now());
    doc.setFileHash(fileHash);
    doc.setContent(content);
    doc.setFilePath(filePath);

    DocumentEntity savedDoc = repo.save(doc);

    // Index with Lucene
    luceneService.indexDocument(
            savedDoc.getId(),
            savedDoc.getFileName(),
            savedDoc.getContent()
    );
}




}
