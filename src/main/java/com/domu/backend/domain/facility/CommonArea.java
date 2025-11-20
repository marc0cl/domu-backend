package com.domu.backend.domain.facility;

import java.util.Objects;

public record CommonArea(
        Long id,
        String name,
        String location,
        Integer capacity,
        String rules,
        String status
) {
    public CommonArea {
        Objects.requireNonNull(name, "name");
    }
}
