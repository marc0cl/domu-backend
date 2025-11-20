package com.domu.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AmenityRequest(
        @NotNull Long communityId,
        @NotBlank String name,
        String description,
        Integer capacity,
        Integer maxDurationMinutes,
        Boolean allowIfInDebt
) {
}
