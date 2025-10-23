package com.domu.backend.domain.facility;

import com.domu.backend.domain.Identifiable;

import java.util.Objects;

public record CommonArea(
        Long id,
        String name,
        String location,
        Integer capacity,
        String rules,
        String status
) implements Identifiable<CommonArea> {
    public CommonArea {
        Objects.requireNonNull(name, "name");
    }

    @Override
    public CommonArea withId(Long newId) {
        return new CommonArea(newId, name, location, capacity, rules, status);
    }
}
