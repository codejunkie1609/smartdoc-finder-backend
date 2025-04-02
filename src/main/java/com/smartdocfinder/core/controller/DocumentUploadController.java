package com.smartdocfinder.core.controller;



import java.io.IOException;
import java.net.http.HttpRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.smartdocfinder.core.service.DocumentUploadService;
import com.smartdocfinder.core.util.Constants;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/files")
public class DocumentUploadController {
    private static final Logger logger = LoggerFactory.getLogger(DocumentUploadController.class);
    @Autowired
    private DocumentUploadService documentUploadService;

  

    @PostMapping(path = "/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
    
        if(file.isEmpty()){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("file is empty or missing");
        }

        String contentType = file.getContentType();
        
        if (!Constants.ALLOWED_CONTENT_TYPES.contains(contentType)) {
            return ResponseEntity.badRequest().body("Unsupported file type: " + contentType);
        }
        try{
            documentUploadService.save(file);
            return ResponseEntity.ok("File uploaded successfully");

        }
        catch(IOException e){
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to process file");
        }
    }
}