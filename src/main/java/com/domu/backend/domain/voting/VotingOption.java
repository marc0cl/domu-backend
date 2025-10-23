package com.domu.backend.domain.voting;

import com.domu.backend.domain.Identifiable;

import java.util.Objects;

public record VotingOption(
        Long id,
        Long votingEventId,
        String text,
        Integer displayOrder,
        String description
) implements Identifiable<VotingOption> {
    public VotingOption {
        Objects.requireNonNull(votingEventId, "votingEventId");
        Objects.requireNonNull(text, "text");
    }

    @Override
    public VotingOption withId(Long newId) {
        return new VotingOption(newId, votingEventId, text, displayOrder, description);
    }
}
