package com.smartdocfinder.core.events;

import java.util.List;

import com.smartdocfinder.core.model.DocumentEntity;

public record DocumentBatchSavedEvent(List<DocumentEntity> savedDocuments){

}
