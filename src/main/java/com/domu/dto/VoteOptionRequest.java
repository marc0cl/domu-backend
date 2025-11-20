package com.domu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VoteOptionRequest(
        @NotNull Long eventId,
        @NotBlank String label
) {
}
