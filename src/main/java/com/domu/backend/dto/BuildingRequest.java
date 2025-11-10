package com.domu.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BuildingRequest(
        @NotNull Long communityId,
        @NotBlank String name,
        String description
) {
}
