package com.domu.backend.domain.voting;

import java.util.Objects;

public record VotingOption(
        Long id,
        Long votingEventId,
        String text,
        Integer displayOrder,
        String description
) {
    public VotingOption {
        Objects.requireNonNull(votingEventId, "votingEventId");
        Objects.requireNonNull(text, "text");
    }
}
