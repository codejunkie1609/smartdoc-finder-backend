package com.smartdocfinder.core.service;

import java.io.File;
import java.io.IOException;
import java.net.http.HttpRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.smartdocfinder.core.controller.DocumentUploadController;
import com.smartdocfinder.core.model.Document;
import com.smartdocfinder.core.repository.DocumentRepository;

@Service
public class DocumentUploadService {
    private static final Logger logger = LoggerFactory.getLogger(DocumentUploadController.class);
    @Autowired
    private ApplicationContext applicationContext;

    private String computeFileHash(byte[] bytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(bytes);
        return HexFormat.of().formatHex(hash);
    }
   

    private static final String UPLOAD_DIR = System.getProperty("user.home") + "/smartdoc-uploads";
    public void save(MultipartFile file, String content) throws IOException, IllegalArgumentException {
        byte[] fileBytes = file.getBytes();
        String fileHash;
    
        try {
            fileHash = computeFileHash(fileBytes);
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
    }
    
    
}

