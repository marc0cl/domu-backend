package com.domu.backend.domain.access;

import com.domu.backend.domain.Identifiable;

import java.util.Objects;

public record ParkingSpace(
        Long id,
        Long buildingId,
        Long unitId,
        String code,
        String tower,
        String level,
        String spaceType,
        Boolean active
) implements Identifiable<ParkingSpace> {
    public ParkingSpace {
        Objects.requireNonNull(buildingId, "buildingId");
        Objects.requireNonNull(code, "code");
    }

    @Override
    public ParkingSpace withId(Long newId) {
        return new ParkingSpace(newId, buildingId, unitId, code, tower, level, spaceType, active);
    }
}
