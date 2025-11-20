package com.domu.domain.core;

import java.util.Objects;

public record Role(
        Long id,
        String name,
        String description,
        String permissionsJson,
        String status
) {
    public Role {
        Objects.requireNonNull(name, "name");
    }
}
