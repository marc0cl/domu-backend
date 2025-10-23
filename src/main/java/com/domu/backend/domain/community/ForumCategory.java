package com.domu.backend.domain.community;

import com.domu.backend.domain.Identifiable;

import java.util.Objects;

public record ForumCategory(
        Long id,
        String name,
        String description,
        String status
) implements Identifiable<ForumCategory> {
    public ForumCategory {
        Objects.requireNonNull(name, "name");
    }

    @Override
    public ForumCategory withId(Long newId) {
        return new ForumCategory(newId, name, description, status);
    }
}
