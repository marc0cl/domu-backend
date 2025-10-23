package com.domu.backend.domain.finance;

import com.domu.backend.domain.Identifiable;

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
) implements Identifiable<DelinquencyRecord> {
    public DelinquencyRecord {
        Objects.requireNonNull(unitId, "unitId");
        Objects.requireNonNull(balance, "balance");
        Objects.requireNonNull(status, "status");
    }

    @Override
    public DelinquencyRecord withId(Long newId) {
        return new DelinquencyRecord(newId, unitId, periodId, balance, daysDelinquent, status, updatedAt);
    }
}
