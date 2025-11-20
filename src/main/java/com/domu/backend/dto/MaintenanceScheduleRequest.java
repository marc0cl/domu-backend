package com.domu.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record MaintenanceScheduleRequest(
        @NotNull Long communityId,
        Long providerId,
        @NotBlank String assetName,
        String description,
        @NotNull LocalDate scheduledDate,
        LocalDate alertDate,
        String status
) {
}
