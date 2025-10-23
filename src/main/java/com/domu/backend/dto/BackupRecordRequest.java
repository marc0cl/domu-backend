package com.domu.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record BackupRecordRequest(
        @NotNull Long communityId,
        LocalDateTime backupDate,
        @NotBlank String storageLocation,
        Integer rpoHours,
        Integer rtoHours,
        LocalDateTime lastRestorationTest
) {
}
