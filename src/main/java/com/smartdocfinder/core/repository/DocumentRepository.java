

package com.smartdocfinder.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smartdocfinder.core.model.Document;



@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    boolean existsByFileHash(String fileHash);
    
}