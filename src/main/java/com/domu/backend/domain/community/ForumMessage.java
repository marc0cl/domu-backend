package com.domu.backend.domain.community;

import com.domu.backend.domain.Identifiable;

import java.time.LocalDateTime;
import java.util.Objects;

public record ForumMessage(
        Long id,
        Long threadId,
        Long authorUserId,
        String content,
        LocalDateTime publishedAt,
        Boolean edited,
        Boolean deleted
) implements Identifiable<ForumMessage> {
    public ForumMessage {
        Objects.requireNonNull(threadId, "threadId");
        Objects.requireNonNull(authorUserId, "authorUserId");
        Objects.requireNonNull(content, "content");
    }

    @Override
    public ForumMessage withId(Long newId) {
        return new ForumMessage(newId, threadId, authorUserId, content, publishedAt, edited, deleted);
    }
}
