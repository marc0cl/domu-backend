package com.domu.domain.community;

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
) {
    public ForumMessage {
        Objects.requireNonNull(threadId, "threadId");
        Objects.requireNonNull(authorUserId, "authorUserId");
        Objects.requireNonNull(content, "content");
    }
}
