package com.domu.backend.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record TicketUpdateRequest(
        @NotNull Long ticketId,
        Long authorId,
        String status,
        String message,
        LocalDateTime createdAt
) {
}
