package com.domu.backend.domain.access;

import com.domu.backend.domain.Identifiable;

import java.time.LocalDateTime;
import java.util.Objects;

public record VisitAuthorization(
        Long id,
        Long visitId,
        Long residentUserId,
        LocalDateTime authorizedAt,
        String channel,
        String notes,
        Boolean active
) implements Identifiable<VisitAuthorization> {
    public VisitAuthorization {
        Objects.requireNonNull(visitId, "visitId");
        Objects.requireNonNull(residentUserId, "residentUserId");
        Objects.requireNonNull(authorizedAt, "authorizedAt");
    }

    @Override
    public VisitAuthorization withId(Long newId) {
        return new VisitAuthorization(newId, visitId, residentUserId, authorizedAt, channel, notes, active);
    }
}
