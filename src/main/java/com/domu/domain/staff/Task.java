package com.domu.domain.staff;

import java.time.LocalDateTime;
import java.util.Objects;

public record Task(
        Long id,
        Long personnelId,
        Long assignedByUserId,
        String description,
        LocalDateTime assignedAt,
        LocalDateTime dueDate,
        String priority,
        String status
) {
    public Task {
        Objects.requireNonNull(personnelId, "personnelId");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(assignedAt, "assignedAt");
    }
}
