package com.domu.backend.domain.access;

import java.time.LocalDateTime;
import java.util.Objects;

public record AccessLog(
        Long id,
        Long visitId,
        LocalDateTime recordedAt,
        String door,
        Long authorizedByUserId,
        String outcome
) {
    public AccessLog {
        Objects.requireNonNull(visitId, "visitId");
        Objects.requireNonNull(recordedAt, "recordedAt");
        Objects.requireNonNull(outcome, "outcome");
    }
}
