package com.domu.backend.domain.finance;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public record DelinquencyRecord(
        Long id,
        Long unitId,
        Long periodId,
        BigDecimal balance,
        Integer daysDelinquent,
        String status,
        LocalDateTime updatedAt
) {
    public DelinquencyRecord {
        Objects.requireNonNull(unitId, "unitId");
        Objects.requireNonNull(balance, "balance");
        Objects.requireNonNull(status, "status");
    }
}
