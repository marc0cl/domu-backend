package com.domu.backend.domain.voting;

import com.domu.backend.domain.Identifiable;

import java.time.LocalDateTime;
import java.util.Objects;

public record Vote(
        Long id,
        Long votingEventId,
        Long userId,
        Long optionId,
        LocalDateTime castAt,
        String comment
) implements Identifiable<Vote> {
    public Vote {
        Objects.requireNonNull(votingEventId, "votingEventId");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(optionId, "optionId");
        Objects.requireNonNull(castAt, "castAt");
    }

    @Override
    public Vote withId(Long newId) {
        return new Vote(newId, votingEventId, userId, optionId, castAt, comment);
    }
}
