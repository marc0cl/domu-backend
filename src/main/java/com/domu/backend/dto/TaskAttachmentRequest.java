package com.domu.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record TaskAttachmentRequest(
        @NotNull Long taskId,
        @NotBlank String url,
        LocalDateTime createdAt
) {
}
