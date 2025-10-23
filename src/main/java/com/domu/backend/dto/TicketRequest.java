package com.domu.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record TicketRequest(
        @NotNull Long communityId,
        @NotNull Long reporterId,
        @NotBlank String title,
        @NotBlank String description,
        @NotBlank String category,
        String priority,
        String status,
        LocalDateTime createdAt
) {
}
