package com.domu.backend.domain.staff;

import java.time.LocalDateTime;
import java.util.Objects;

public record Shift(
        Long id,
        Long personnelId,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        String observations
) {
    public Shift {
        Objects.requireNonNull(personnelId, "personnelId");
        Objects.requireNonNull(startDateTime, "startDateTime");
    }
}
