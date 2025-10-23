package com.domu.backend.domain.access;

import com.domu.backend.domain.Identifiable;

import java.time.LocalDateTime;
import java.util.Objects;

public record AccessLog(
        Long id,
        Long visitId,
        LocalDateTime recordedAt,
        String door,
        Long authorizedByUserId,
        String outcome
) implements Identifiable<AccessLog> {
    public AccessLog {
        Objects.requireNonNull(visitId, "visitId");
        Objects.requireNonNull(recordedAt, "recordedAt");
        Objects.requireNonNull(outcome, "outcome");
    }

    @Override
    public AccessLog withId(Long newId) {
        return new AccessLog(newId, visitId, recordedAt, door, authorizedByUserId, outcome);
    }
}
