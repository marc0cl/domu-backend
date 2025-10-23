package com.domu.backend.domain.core;

import com.domu.backend.domain.Identifiable;

import java.time.LocalDateTime;
import java.util.Objects;

public record Community(
        Long id,
        String name,
        String description,
        LocalDateTime createdAt,
        String status
) implements Identifiable<Community> {
    public Community {
        Objects.requireNonNull(name, "name");
    }

    @Override
    public Community withId(Long newId) {
        return new Community(newId, name, description, createdAt, status);
    }
}
