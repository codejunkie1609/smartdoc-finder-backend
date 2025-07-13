package com.smartdocfinder.core.service; // Use your correct package

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class LuceneIndexRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(LuceneIndexRunner.class);
    private static final int BATCH_SIZE = 50; // The number of files to process in each batch
    private static final int CONCURRENT_BATCHES = 20; // Number of concurrent batches to process

    @Value("${indexing.source-path}")
    private String sourceDirectoryPath;

    @Autowired
    private DocumentUploadService documentUploadService;

    @Override
    public void run(String... args) throws Exception {
        logger.info("================================================");
        logger.info("   AUTO-INDEXER: Starting indexing on boot...   ");
        logger.info("================================================");

        Path targetDir = Paths.get(sourceDirectoryPath);
        if (!Files.exists(targetDir) || !Files.isDirectory(targetDir)) {
            logger.error("Directory not found: {}. Please check the path.", sourceDirectoryPath);
            return;
        }

        // --- REFACTORED LOGIC ---

        // 1. Collect all file paths first.
        List<Path> allFiles;
        try (Stream<Path> stream = Files.walk(targetDir)) {
            allFiles = stream.filter(Files::isRegularFile).collect(Collectors.toList());
        }

        if (allFiles.isEmpty()) {
            logger.warn("No files found to index in the specified directory.");
            return;
        }
        
        logger.info("Found {} total files. Processing in batches of {}...", allFiles.size(), BATCH_SIZE);

        // 2. Partition the list of files into smaller batches.
        List<List<Path>> batches = Lists.partition(allFiles, BATCH_SIZE);

        // 3. Create a virtual thread executor to process batches in parallel.
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_BATCHES);

        for (int i = 0; i < batches.size(); i++) {
            final int batchNumber = i + 1;
            final List<Path> currentBatch = batches.get(i);
            
            // 4. Submit each BATCH as a single task.
            executor.submit(() -> {
                logger.info("Processing batch {} of {} ({} files)...", batchNumber, batches.size(), currentBatch.size());
                try {
                    documentUploadService.processDocumentBatch(currentBatch);
                } catch (Exception e) {
                    logger.error("An unexpected error occurred while processing batch {}.", batchNumber, e);
                }
            });
        }
        
        // 5. Gracefully shut down the executor.
        shutdownExecutor(executor);

        logger.info("================================================");
        logger.info("   AUTO-INDEXER: All batch processing finished. ");
        logger.info("================================================");
    }
    
    private void shutdownExecutor(ExecutorService executor) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(1, TimeUnit.HOURS)) {
                logger.error("Indexing timed out after 1 hour. Forcing shutdown...");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.error("Indexing was interrupted. Forcing shutdown...");
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}