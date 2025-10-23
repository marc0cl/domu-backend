package com.domu.backend.domain.staff;

import com.domu.backend.domain.Identifiable;

import java.time.LocalDateTime;
import java.util.Objects;

public record PersonnelLog(
        Long id,
        Long personnelId,
        LocalDateTime occurredAt,
        String detail,
        String eventType
) implements Identifiable<PersonnelLog> {
    public PersonnelLog {
        Objects.requireNonNull(personnelId, "personnelId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(detail, "detail");
    }

    @Override
    public PersonnelLog withId(Long newId) {
        return new PersonnelLog(newId, personnelId, occurredAt, detail, eventType);
    }
}
