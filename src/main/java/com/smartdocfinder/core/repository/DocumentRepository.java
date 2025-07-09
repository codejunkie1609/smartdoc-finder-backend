

package com.smartdocfinder.core.repository;

import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.smartdocfinder.core.model.DocumentEntity;



@Repository
public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {
    boolean existsByFileHash(String fileHash);

    @Query("SELECT d.fileHash FROM DocumentEntity d WHERE d.fileHash IN :hashes")
    Set<String> findExistingHashes(@Param("hashes") Set<String> hashes);
    
}