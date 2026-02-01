package com.domu.dto;

import java.time.LocalDateTime;

public record IncidentResponse(
                Long id,
                Long userId,
                Long unitId,
                Long buildingId,
                String title,
                String description,
                String category,
                String priority,
                String status,
                Long assignedToUserId,
                LocalDateTime createdAt,
                LocalDateTime updatedAt) {
}