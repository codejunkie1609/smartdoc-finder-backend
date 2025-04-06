package com.smartdocfinder.core.service;

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
import com.smartdocfinder.core.model.Document;
import com.smartdocfinder.core.repository.DocumentRepository;
import com.smartdocfinder.core.util.Utilities;

@Service
public class DocumentUploadService {
    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);
    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private LuceneService luceneService;

    private static final String UPLOAD_DIR = System.getProperty("user.home") + "/smartdoc-uploads";

    public void save(MultipartFile file) throws IOException, IllegalArgumentException, TikaException {
        byte[] fileBytes = file.getBytes();
        InputStream stream = file.getInputStream();
        String mediaType = DocumentParserService.detectMediaType(fileBytes);
        logger.info("media type: {}", mediaType);
        if (!Constants.ALLOWED_TYPES.contains(mediaType)) {
            throw new IllegalArgumentException("Unsupported file type: " + mediaType);
        }

        String originalFileName = file.getOriginalFilename();
        String extension = originalFileName != null && originalFileName.contains(".")
                ? originalFileName.substring(originalFileName.lastIndexOf('.') + 1).toLowerCase()
                : "";
        logger.info("File extension: {}", extension);

        if (!Constants.ALLOWED_EXTENSIONS.contains(extension)) {
            logger.info("Unsupported file extension: {}", extension);
            throw new UnsupportedFormatException("not of expected extension");
        }
        String content = DocumentParserService.extractContent(stream);
        String fileHash;

        try {
            fileHash = Utilities.computeFileHash(fileBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error computing file hash", e);
        }

        DocumentRepository repo = applicationContext.getBean(DocumentRepository.class);
        if (repo.existsByFileHash(fileHash)) {
            throw new IllegalArgumentException("File already exists");
        }

        String safeName = System.currentTimeMillis() + "_" + originalFileName;

        Path uploadDir = Paths.get(UPLOAD_DIR);
        Files.createDirectories(uploadDir);
        Path path = uploadDir.resolve(safeName);
        Files.write(path, fileBytes);

        Document doc = new Document();
        doc.setFileName(originalFileName);
        doc.setFilePath(path.toAbsolutePath().toString());
        doc.setFileType(file.getContentType());
        doc.setFileSize(file.getSize());
        doc.setUploadedAt(LocalDateTime.now());
        doc.setFileHash(fileHash);
        doc.setContent(content);
        Document savedDoc = repo.save(doc);

        luceneService.indexDocument(
                savedDoc.getId(),
                savedDoc.getFileName(),
                savedDoc.getContent());
        stream.close();
    }

}
