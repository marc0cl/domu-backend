package com.domu.backend.domain.ticket;

import com.domu.backend.domain.Identifiable;

import java.time.LocalDateTime;
import java.util.Objects;

public record TicketFollowUp(
        Long id,
        Long ticketId,
        Long userId,
        LocalDateTime occurredAt,
        String comment,
        String status
) implements Identifiable<TicketFollowUp> {
    public TicketFollowUp {
        Objects.requireNonNull(ticketId, "ticketId");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }

    @Override
    public TicketFollowUp withId(Long newId) {
        return new TicketFollowUp(newId, ticketId, userId, occurredAt, comment, status);
    }
}
