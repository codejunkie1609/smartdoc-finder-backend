package com.smartdocfinder.core.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.smartdocfinder.core.model.Document;
import com.smartdocfinder.core.repository.DocumentRepository;

@Service
public class DocumentUploadService {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DocumentRepository documentRepository;

    private static final String UPLOAD_DIR = "uploads";
    public void save(MultipartFile file) throws IOException {
        File uploadDir = new File(UPLOAD_DIR);
        if(!uploadDir.exists()){
            uploadDir.mkdirs();
        }

        String originalFileName = file.getName();
        String safeName = System.currentTimeMillis()+"_"+originalFileName;
        Path filePath = Paths.get(UPLOAD_DIR, safeName);
        file.transferTo(filePath.toFile());

      
        Document doc = new Document();
        doc.setFileName(originalFileName);
        doc.setFilePath(filePath.toString());
        doc.setFileType(file.getContentType());
        doc.setFileSize(file.getSize());
        doc.setUploadedAt(LocalDateTime.now());

        
        documentRepository.save(doc);
    }
    
}

