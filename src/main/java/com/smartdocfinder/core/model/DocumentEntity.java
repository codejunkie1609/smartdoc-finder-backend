package com.smartdocfinder.core.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "documents")
public class DocumentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter @Setter private Long id;

    @Getter @Setter private String fileName;
    @Getter @Setter private String filePath;
    @Getter @Setter private Long fileSize;
    @Getter @Setter private String fileType;
    @Getter @Setter private LocalDateTime uploadedAt = LocalDateTime.now();

    @Getter @Setter
    @Column(unique = true)
    private String fileHash; 

    @Getter @Setter
    @Column(columnDefinition = "TEXT")
    private String content;
}

