package com.domu.domain.core;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public record HousingUnit(
        Long id,
        Long buildingId,
        String number,
        String tower,
        String floor,
        BigDecimal aliquotPercentage,
        BigDecimal squareMeters,
        String status,
        Long createdByUserId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
    public HousingUnit {
        Objects.requireNonNull(buildingId, "buildingId");
        Objects.requireNonNull(number, "number");
    }
}
