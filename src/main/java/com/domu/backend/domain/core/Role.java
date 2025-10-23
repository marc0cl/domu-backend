package com.domu.backend.domain.core;

import com.domu.backend.domain.Identifiable;

import java.util.Objects;

public record Role(
        Long id,
        String name,
        String description,
        String permissionsJson,
        String status
) implements Identifiable<Role> {
    public Role {
        Objects.requireNonNull(name, "name");
    }

    @Override
    public Role withId(Long newId) {
        return new Role(newId, name, description, permissionsJson, status);
    }
}
