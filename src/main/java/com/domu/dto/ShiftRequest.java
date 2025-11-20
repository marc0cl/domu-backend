package com.domu.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ShiftRequest(
        @NotNull Long staffId,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        String notes
) {
}
