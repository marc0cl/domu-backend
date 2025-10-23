package com.domu.backend.domain.community;

import com.domu.backend.domain.Identifiable;

import java.time.LocalDateTime;
import java.util.Objects;

public record Notification(
        Long id,
        Long userId,
        String title,
        String message,
        LocalDateTime createdAt,
        Boolean read
) implements Identifiable<Notification> {
    public Notification {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    @Override
    public Notification withId(Long newId) {
        return new Notification(newId, userId, title, message, createdAt, read);
    }
}
