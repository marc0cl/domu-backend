package com.domu.domain.vendor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

public record ServiceOrder(
        Long id,
        Long buildingId,
        Long providerId,
        Long createdBy,
        String title,
        String description,
        LocalDate scheduledDate,
        String status,
        String priority,
        String adminNotes,
        String providerNotes,
        LocalDateTime completedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public ServiceOrder {
        Objects.requireNonNull(buildingId, "buildingId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(createdBy, "createdBy");
        Objects.requireNonNull(title, "title");
    }
}
