package com.domu.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record MaintenanceLogRequest(
        @NotNull Long scheduleId,
        String notes,
        LocalDateTime completedAt,
        String attachmentUrl
) {
}
