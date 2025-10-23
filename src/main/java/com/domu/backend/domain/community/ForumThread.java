package com.domu.backend.domain.community;

import com.domu.backend.domain.Identifiable;

import java.time.LocalDateTime;
import java.util.Objects;

public record ForumThread(
        Long id,
        Long categoryId,
        Long buildingId,
        String title,
        LocalDateTime createdAt,
        Boolean pinned,
        String status
) implements Identifiable<ForumThread> {
    public ForumThread {
        Objects.requireNonNull(categoryId, "categoryId");
        Objects.requireNonNull(buildingId, "buildingId");
        Objects.requireNonNull(title, "title");
    }

    @Override
    public ForumThread withId(Long newId) {
        return new ForumThread(newId, categoryId, buildingId, title, createdAt, pinned, status);
    }
}
