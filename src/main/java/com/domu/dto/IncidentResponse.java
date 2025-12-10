package com.domu.dto;

import java.time.LocalDateTime;

public record IncidentResponse(
        Long id,
        Long userId,
        Long unitId,
        String title,
        String description,
        String category,
        String priority,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

