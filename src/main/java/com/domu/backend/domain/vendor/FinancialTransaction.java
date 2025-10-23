package com.domu.backend.domain.vendor;

import com.domu.backend.domain.Identifiable;

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
) implements Identifiable<FinancialTransaction> {
    public FinancialTransaction {
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(amount, "amount");
    }

    @Override
    public FinancialTransaction withId(Long newId) {
        return new FinancialTransaction(newId, providerId, date, type, amount, method, notes);
    }
}
