package com.domu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ForumPostRequest(
        @NotNull Long threadId,
        @NotNull Long authorId,
        @NotBlank String content,
        LocalDateTime createdAt
) {
}
