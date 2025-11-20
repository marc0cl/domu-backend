package com.domu.domain.staff;

import java.time.LocalDateTime;
import java.util.Objects;

public record PersonnelLog(
        Long id,
        Long personnelId,
        LocalDateTime occurredAt,
        String detail,
        String eventType
) {
    public PersonnelLog {
        Objects.requireNonNull(personnelId, "personnelId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(detail, "detail");
    }
}
