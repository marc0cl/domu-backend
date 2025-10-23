package com.domu.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UnitRequest(
        @NotNull Long buildingId,
        @NotBlank String number,
        String floor,
        BigDecimal areaM2,
        String status
) {
}
