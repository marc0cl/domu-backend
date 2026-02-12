package com.domu.dto;

import com.domu.domain.LibraryDocument;
import java.time.LocalDateTime;

public record LibraryDocumentResponse(
    Long id,
    String name,
    String category,
    String fileName,
    String fileUrl,
    Long size,
    LocalDateTime uploadDate,
    Long uploadedBy
) {
    public static LibraryDocumentResponse from(LibraryDocument doc) {
        return new LibraryDocumentResponse(
            doc.id(),
            doc.name(),
            doc.category(),
            doc.fileName(),
            doc.fileUrl(),
            doc.size(),
            doc.uploadDate(),
            doc.uploadedBy()
        );
    }
}
