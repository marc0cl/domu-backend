package com.domu.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ForumThreadRequest(
        @NotNull Long communityId,
        Long categoryId,
        @NotNull Long authorId,
        @NotBlank String title,
        LocalDateTime createdAt
) {
}
