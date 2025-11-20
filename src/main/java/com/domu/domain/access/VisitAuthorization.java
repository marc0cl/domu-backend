package com.domu.domain.access;

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
) {
    public VisitAuthorization {
        Objects.requireNonNull(visitId, "visitId");
        Objects.requireNonNull(residentUserId, "residentUserId");
        Objects.requireNonNull(authorizedAt, "authorizedAt");
    }
}
