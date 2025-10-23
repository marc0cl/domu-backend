package com.domu.backend.domain.staff;

import com.domu.backend.domain.Identifiable;

import java.time.LocalDateTime;
import java.util.Objects;

public record Shift(
        Long id,
        Long personnelId,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        String observations
) implements Identifiable<Shift> {
    public Shift {
        Objects.requireNonNull(personnelId, "personnelId");
        Objects.requireNonNull(startDateTime, "startDateTime");
    }

    @Override
    public Shift withId(Long newId) {
        return new Shift(newId, personnelId, startDateTime, endDateTime, observations);
    }
}
