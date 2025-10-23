package com.domu.backend.domain.core;

import com.domu.backend.domain.Identifiable;

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
) implements Identifiable<HousingUnit> {
    public HousingUnit {
        Objects.requireNonNull(buildingId, "buildingId");
        Objects.requireNonNull(number, "number");
    }

    @Override
    public HousingUnit withId(Long newId) {
        return new HousingUnit(
                newId,
                buildingId,
                number,
                tower,
                floor,
                aliquotPercentage,
                squareMeters,
                status
        );
    }
}
