package com.smartdocfinder.core.controller;



import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpRequest;
import java.util.Set;

import org.apache.tika.exception.TikaException;
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

import com.smartdocfinder.core.service.DocumentParserService;
import com.smartdocfinder.core.service.DocumentUploadService;
import com.smartdocfinder.core.util.Constants;

@RestController
@RequestMapping("/api/files")
public class DocumentUploadController {
    private static final Logger logger = LoggerFactory.getLogger(DocumentUploadController.class);
    @Autowired
    private DocumentUploadService documentUploadService;

    

    @PostMapping(path = "/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {    
        if(file.isEmpty()){
            logger.info("file is empty!");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("file is empty or missing");
        }

        String contentType = file.getContentType();
        
        if (!Constants.ALLOWED_TYPES.contains(contentType)) {
            logger.info("content is of Unsupported type: {}", contentType);
            return ResponseEntity.badRequest().body("Unsupported file type: " + contentType);
        }
        
        try{
            documentUploadService.save(file);
            logger.info("file uploaded successfully!");
            return ResponseEntity.ok("File uploaded successfully");

        }
        catch(IOException e){
            logger.error("I/O error occurred! {}" , e.getMessage());
            return ResponseEntity.internalServerError().body("Failed to process file");
        }
        catch(IllegalArgumentException e){
            logger.error("Illegal argument error occurred! {}" , e.getMessage());
            return ResponseEntity.internalServerError().body("File exists/ is of an unsupported type");
        } catch (TikaException e) {
            logger.error("Illegal argument error occurred! {}" , e.getMessage());
            return ResponseEntity.internalServerError().body("Tika Error");
                } 
               
    }
}