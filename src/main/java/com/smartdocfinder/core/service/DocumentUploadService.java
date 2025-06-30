package com.smartdocfinder.core.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import org.apache.tika.exception.TikaException;
import org.apache.tika.exception.UnsupportedFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.smartdocfinder.core.controller.Constants;
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
        throws IOException, TikaException {

    byte[] fileBytes = stream.readAllBytes();

    // Validate MIME type
    String mediaType = DocumentParserService.detectMediaType(fileBytes);
    logger.info("media type: {}", mediaType);
    if (!Constants.ALLOWED_TYPES.contains(mediaType)) {
        throw new IllegalArgumentException("Unsupported file type: " + mediaType);
    }

    // Validate extension
    String extension = originalFileName != null && originalFileName.contains(".")
            ? originalFileName.substring(originalFileName.lastIndexOf('.') + 1).toLowerCase()
            : "";
    logger.info("File extension: {}", extension);
    if (!Constants.ALLOWED_EXTENSIONS.contains(extension)) {
        logger.info("Unsupported file extension: {}", extension);
        throw new UnsupportedFormatException("Not of expected extension: " + extension);
    }

    // Extract content
    String content = DocumentParserService.extractContent(new ByteArrayInputStream(fileBytes));

    // OPTIONAL: Deduplication
    String fileHash;
    try {
        fileHash = Utilities.computeFileHash(fileBytes);
    } catch (Exception e) {
        throw new RuntimeException("Error computing file hash", e);
    }

    DocumentRepository repo = applicationContext.getBean(DocumentRepository.class);
    if (repo.existsByFileHash(fileHash)) {
        throw new IllegalArgumentException("File already indexed");
    }

    // Persist minimal metadata
    DocumentEntity doc = new DocumentEntity();
    doc.setFileName(originalFileName);
    doc.setFileType(contentType);
    doc.setUploadedAt(LocalDateTime.now());
    doc.setFileHash(fileHash);
    doc.setContent(content);
    doc.setFilePath(filePath);  // ✅ Store full file path

    DocumentEntity savedDoc = repo.save(doc);

    // Index with Lucene
    luceneService.indexDocument(
            savedDoc.getId(),
            savedDoc.getFileName(),
            savedDoc.getContent());
}


}
