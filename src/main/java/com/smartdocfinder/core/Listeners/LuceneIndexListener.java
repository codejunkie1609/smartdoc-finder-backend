package com.smartdocfinder.core.Listeners;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import com.smartdocfinder.core.events.DocumentBatchSavedEvent;
import com.smartdocfinder.core.model.DocumentEntity;
import com.smartdocfinder.core.service.LuceneService;

import lombok.RequiredArgsConstructor;

@Component  
@RequiredArgsConstructor
public class LuceneIndexListener {
    private static final Logger logger = LoggerFactory.getLogger(LuceneIndexListener.class);

    @Autowired
    private LuceneService luceneService;

    // ✅ This method ALSO runs automatically after the transaction commits
    @TransactionalEventListener
    public void handleDocumentBatchSaved(DocumentBatchSavedEvent event) {
        for (DocumentEntity doc : event.savedDocuments()) {
            try {
                luceneService.indexDocument(doc.getId(), doc.getFileName(), doc.getContent());
            } catch (Exception e) {
                logger.error("Failed to index document ID: {}", doc.getId(), e);
            }
        }
    }
}
