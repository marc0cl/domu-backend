package com.domu.dto;

import java.time.LocalDateTime;

public record VisitContactResponse(
        Long id,
        String visitorName,
        String visitorDocument,
        Long unitId,
        String alias,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

