package com.domu.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ParkingSpaceRequest(
        @NotNull Long communityId,
        Long unitId,
        @NotBlank String code,
        String spaceType,
        Boolean active
) {
}
