package com.domu.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record TaskRequest(
        @NotNull Long communityId,
        @NotBlank String title,
        String description,
        Long assigneeId,
        String status,
        String priority,
        LocalDateTime dueDate,
        LocalDateTime completedAt
) {
}
