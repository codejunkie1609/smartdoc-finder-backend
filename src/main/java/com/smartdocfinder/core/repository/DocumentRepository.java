

package com.smartdocfinder.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smartdocfinder.core.model.DocumentEntity;



@Repository
public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {
    boolean existsByFileHash(String fileHash);
    
}