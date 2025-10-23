package com.domu.backend.domain.operations;

import com.domu.backend.domain.Identifiable;

import java.time.LocalDateTime;
import java.util.Objects;

public record BackupRecord(
        Long id,
        LocalDateTime executedAt,
        String location,
        String status,
        String initiatedBy
) implements Identifiable<BackupRecord> {
    public BackupRecord {
        Objects.requireNonNull(executedAt, "executedAt");
        Objects.requireNonNull(location, "location");
    }

    @Override
    public BackupRecord withId(Long newId) {
        return new BackupRecord(newId, executedAt, location, status, initiatedBy);
    }
}
