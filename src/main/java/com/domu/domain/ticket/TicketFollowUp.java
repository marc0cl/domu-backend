package com.domu.domain.ticket;

import java.time.LocalDateTime;
import java.util.Objects;

public record TicketFollowUp(
        Long id,
        Long ticketId,
        Long userId,
        LocalDateTime occurredAt,
        String comment,
        String status
) {
    public TicketFollowUp {
        Objects.requireNonNull(ticketId, "ticketId");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
