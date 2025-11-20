package com.domu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record VoteEventRequest(
        @NotNull Long communityId,
        @NotBlank String title,
        String description,
        @NotNull LocalDateTime startAt,
        @NotNull LocalDateTime endAt,
        Boolean anonymous
) {
}
