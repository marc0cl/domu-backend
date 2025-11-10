package com.domu.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record DeliveryRequest(
        @NotNull Long communityId,
        @NotNull Long residentId,
        @NotBlank String packageCode,
        LocalDateTime receivedAt,
        LocalDateTime deliveredAt,
        String evidencePhotoUrl,
        String signature,
        String status
) {
}
