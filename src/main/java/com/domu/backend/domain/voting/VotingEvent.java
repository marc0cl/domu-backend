package com.domu.backend.domain.voting;

import java.time.LocalDateTime;
import java.util.Objects;

public record VotingEvent(
        Long id,
        String topic,
        String description,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        Integer quorum,
        String status
) {
    public VotingEvent {
        Objects.requireNonNull(topic, "topic");
        Objects.requireNonNull(startDateTime, "startDateTime");
        Objects.requireNonNull(endDateTime, "endDateTime");
    }
}
