package com.domu.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record VoteRequest(
        @NotNull Long eventId,
        @NotNull Long optionId,
        @NotNull Long residentId,
        LocalDateTime castAt,
        String verificationHash
) {
}
