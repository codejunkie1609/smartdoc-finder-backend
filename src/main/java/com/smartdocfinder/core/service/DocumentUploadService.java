package com.smartdocfinder.core.service;


import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import org.apache.poi.UnsupportedFileFormatException;
import org.apache.tika.exception.TikaException;
import org.apache.tika.exception.UnsupportedFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.smartdocfinder.core.controller.DocumentUploadController;
import com.smartdocfinder.core.model.Document;
import com.smartdocfinder.core.repository.DocumentRepository;
import com.smartdocfinder.core.util.Constants;
import com.smartdocfinder.core.util.Utilities;

@Service
public class DocumentUploadService {
    private static final Logger logger = LoggerFactory.getLogger(DocumentUploadController.class);
    @Autowired
    private ApplicationContext applicationContext;

    private static final String UPLOAD_DIR = System.getProperty("user.home") + "/smartdoc-uploads";
    public void save(MultipartFile file) throws IOException, IllegalArgumentException, TikaException {
        byte[] fileBytes = file.getBytes();
        InputStream stream = file.getInputStream();
        String mediaType = DocumentParserService.detectMediaType(fileBytes);
        logger.info("media type: {}", mediaType);
        if (!Constants.ALLOWED_TYPES.contains(mediaType)) {
            throw new IllegalArgumentException("Unsupported file type: " + mediaType);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase()
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
    
        String originalFileName = file.getOriginalFilename();
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
        repo.save(doc);
        stream.close();
    }
    
    
}

