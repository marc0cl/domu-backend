package com.domu.backend.domain.voting;

import java.time.LocalDateTime;
import java.util.Objects;

public record Vote(
        Long id,
        Long votingEventId,
        Long userId,
        Long optionId,
        LocalDateTime castAt,
        String comment
) {
    public Vote {
        Objects.requireNonNull(votingEventId, "votingEventId");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(optionId, "optionId");
        Objects.requireNonNull(castAt, "castAt");
    }
}
