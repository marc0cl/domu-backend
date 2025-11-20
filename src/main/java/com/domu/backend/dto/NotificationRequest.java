package com.domu.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record NotificationRequest(
        @NotNull Long communityId,
        Long residentId,
        @NotBlank String title,
        @NotBlank String message,
        String channel,
        String data,
        LocalDateTime createdAt,
        LocalDateTime deliveredAt
) {
}
