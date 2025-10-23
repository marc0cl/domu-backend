package com.domu.backend.domain.voting;

import com.domu.backend.domain.Identifiable;

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
) implements Identifiable<VotingEvent> {
    public VotingEvent {
        Objects.requireNonNull(topic, "topic");
        Objects.requireNonNull(startDateTime, "startDateTime");
        Objects.requireNonNull(endDateTime, "endDateTime");
    }

    @Override
    public VotingEvent withId(Long newId) {
        return new VotingEvent(newId, topic, description, startDateTime, endDateTime, quorum, status);
    }
}
