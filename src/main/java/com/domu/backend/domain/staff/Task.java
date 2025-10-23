package com.domu.backend.domain.staff;

import com.domu.backend.domain.Identifiable;

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
) implements Identifiable<Task> {
    public Task {
        Objects.requireNonNull(personnelId, "personnelId");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(assignedAt, "assignedAt");
    }

    @Override
    public Task withId(Long newId) {
        return new Task(newId, personnelId, assignedByUserId, description, assignedAt, dueDate, priority, status);
    }
}
