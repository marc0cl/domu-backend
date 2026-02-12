package com.domu.domain;

import java.time.LocalDateTime;

public record LibraryDocument(
    Long id,
    Long buildingId,
    String name,
    String category,
    String fileName,
    String fileUrl,
    Long size,
    LocalDateTime uploadDate,
    Long uploadedBy
) {}
