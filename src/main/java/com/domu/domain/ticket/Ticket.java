package com.domu.domain.ticket;

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
) {
    public Ticket {
        Objects.requireNonNull(createdByUserId, "createdByUserId");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(priority, "priority");
    }
}
