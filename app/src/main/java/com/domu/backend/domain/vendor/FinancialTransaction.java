package com.domu.backend.domain.vendor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public record FinancialTransaction(
        Long id,
        Long providerId,
        LocalDate date,
        String type,
        BigDecimal amount,
        String method,
        String notes
) {
    public FinancialTransaction {
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(amount, "amount");
    }
}
