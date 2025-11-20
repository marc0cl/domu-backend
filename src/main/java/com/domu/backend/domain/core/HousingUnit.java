package com.domu.backend.domain.core;

import java.math.BigDecimal;
import java.util.Objects;

public record HousingUnit(
        Long id,
        Long buildingId,
        String number,
        String tower,
        String floor,
        BigDecimal aliquotPercentage,
        BigDecimal squareMeters,
        String status
) {
    public HousingUnit {
        Objects.requireNonNull(buildingId, "buildingId");
        Objects.requireNonNull(number, "number");
    }
}
