package com.domu.backend.domain.community;

import java.util.Objects;

public record ForumCategory(
        Long id,
        String name,
        String description,
        String status
) {
    public ForumCategory {
        Objects.requireNonNull(name, "name");
    }
}
