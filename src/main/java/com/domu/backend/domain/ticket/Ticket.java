package com.domu.backend.domain.ticket;

import com.domu.backend.domain.Identifiable;

import java.time.LocalDateTime;
import java.util.Objects;

public record Ticket(
        Long id,
        Long createdByUserId,
        String subject,
        String description,
        String priority,
        String category,
        LocalDateTime createdAt,
        LocalDateTime closedAt,
        String status
) implements Identifiable<Ticket> {
    public Ticket {
        Objects.requireNonNull(createdByUserId, "createdByUserId");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(priority, "priority");
    }

    @Override
    public Ticket withId(Long newId) {
        return new Ticket(newId, createdByUserId, subject, description, priority, category, createdAt, closedAt, status);
    }
}
