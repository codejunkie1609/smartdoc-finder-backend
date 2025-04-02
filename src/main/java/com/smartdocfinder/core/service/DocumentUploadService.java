package com.smartdocfinder.core.service;

import java.io.File;
import java.io.IOException;
import java.net.http.HttpRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.smartdocfinder.core.controller.DocumentUploadController;
import com.smartdocfinder.core.model.Document;
import com.smartdocfinder.core.repository.DocumentRepository;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class DocumentUploadService {
    private static final Logger logger = LoggerFactory.getLogger(DocumentUploadController.class);
    @Autowired
    private ApplicationContext applicationContext;


   

    private static final String UPLOAD_DIR = System.getProperty("user.home") + "/smartdoc-uploads";
    public void save(MultipartFile file) throws IOException {
        String originalFileName = file.getOriginalFilename();
        String safeName = System.currentTimeMillis() + "_" + originalFileName;
    
        // Ensure directory exists
        Path uploadDir = Paths.get(UPLOAD_DIR);
        Files.createDirectories(uploadDir);
    
        // Define final path
        Path path = uploadDir.resolve(safeName);
    
        // Write file to disk
        Files.write(path, file.getBytes());
    
        // Save document metadata
        Document doc = new Document();
        doc.setFileName(originalFileName);
        doc.setFilePath(path.toAbsolutePath().toString());
        doc.setFileType(file.getContentType());
        doc.setFileSize(file.getSize());
        doc.setUploadedAt(LocalDateTime.now());
    
        applicationContext.getBean(DocumentRepository.class).save(doc);
    }
    
    
}

