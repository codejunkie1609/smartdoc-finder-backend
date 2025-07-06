package com.smartdocfinder.core.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.stream.Stream;

@Component
public class LuceneIndexRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(LuceneIndexRunner.class);

    // This injects the path from the environment variable in docker-compose
    @Value("${indexing.source-path}")
    private String sourceDirectoryPath;

    // Autowire the service that contains your `parseAndIndex` method
    @Autowired
    private DocumentUploadService documentUploadService;

    @Override
    public void run(String... args) throws Exception {
        logger.info("================================================");
        logger.info("   AUTO-INDEXER: Starting indexing on boot...   ");
        logger.info("================================================");

        if (sourceDirectoryPath == null || sourceDirectoryPath.isBlank()) {
            logger.error("Indexing source path is not set. Please configure INDEXING_SOURCE_PATH.");
            return;
        }

        Path targetDir = Paths.get(sourceDirectoryPath);
        if (!Files.exists(targetDir) || !Files.isDirectory(targetDir)) {
            logger.error("Directory not found: {}. Please check the path.", sourceDirectoryPath);
            return;
        }

        logger.info("Scanning for documents in: {}", targetDir.toAbsolutePath());

        long totalFiles;
        long indexedFiles = 0;
        try (Stream<Path> fileStream = Files.walk(targetDir)) {
            // Use an iterator to process files one by one
            Iterator<Path> fileIterator = fileStream.filter(Files::isRegularFile).iterator();
            if (!fileIterator.hasNext()) {
                logger.warn("No files found to index in the specified directory.");
                return;
            }

            while (fileIterator.hasNext()) {
                Path file = fileIterator.next();
                logger.info("Indexing file: {}", file.getFileName());
                indexedFiles++;

                try (InputStream in = Files.newInputStream(file)) {
                    String contentType = Files.probeContentType(file);
                    documentUploadService.parseAndIndex(
                            in,
                            file.getFileName().toString(),
                            contentType,
                            file.toString()
                    );
                } catch (Exception e) {
                    logger.warn("--> Failed to index file: {}. Reason: {}", file.getFileName(), e.getMessage());
                }
            }
        }
        logger.info("================================================");
        logger.info("   AUTO-INDEXER: Finished. Indexed {} files.    ", indexedFiles);
        logger.info("================================================");
    }
}
