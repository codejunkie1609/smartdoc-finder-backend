
package com.smartdocfinder.core.Listeners;

import com.smartdocfinder.core.config.RabbitMQConfig;
import com.smartdocfinder.core.events.DocumentBatchSavedEvent;
import com.smartdocfinder.core.model.DocumentEntity;
import java.util.Map;
import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class EmbeddingMessageListener {
    private static final Logger logger = LoggerFactory.getLogger(EmbeddingMessageListener.class); // ✅ Add logger
    private final RabbitTemplate rabbitTemplate;

    @TransactionalEventListener
    public void handleDocumentBatchSavedEvent(DocumentBatchSavedEvent event) {
        for(DocumentEntity doc: event.savedDocuments()) {
            // Prepare the message with the document ID and content
            Map<String, Object> message = Map.of(
                "documentId", doc.getId(),
                "content", doc.getContent()
            );
             logger.debug("Sending message to RabbitMQ: {}", message);

            // Send the message to the RabbitMQ queue for embedding processing
            rabbitTemplate.convertAndSend(RabbitMQConfig.QUEUE_NAME, message);


        }
    }
}