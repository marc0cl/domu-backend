package com.domu.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ProviderServiceRequest(
        @NotNull Long communityId,
        @NotNull Long providerId,
        @NotBlank String description,
        String status,
        String quotationUrl,
        LocalDateTime createdAt
) {
}
