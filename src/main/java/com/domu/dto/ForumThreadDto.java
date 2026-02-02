package com.domu.dto;

import java.time.LocalDateTime;

public record ForumThreadDto(
    Long id,
    String title,
    String content,
    String category,
    String categoryLabel,
    String categoryIcon,
    LocalDateTime date,
    Long authorId,
    String authorName,
    boolean pinned
) {}