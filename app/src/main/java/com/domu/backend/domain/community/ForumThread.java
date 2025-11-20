package com.domu.backend.domain.community;

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
) {
    public ForumThread {
        Objects.requireNonNull(categoryId, "categoryId");
        Objects.requireNonNull(buildingId, "buildingId");
        Objects.requireNonNull(title, "title");
    }
}
