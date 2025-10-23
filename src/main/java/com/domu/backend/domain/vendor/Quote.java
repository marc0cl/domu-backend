package com.domu.backend.domain.vendor;

import com.domu.backend.domain.Identifiable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public record Quote(
        Long id,
        Long serviceRequestId,
        LocalDate date,
        BigDecimal amount,
        String conditions,
        String status
) implements Identifiable<Quote> {
    public Quote {
        Objects.requireNonNull(serviceRequestId, "serviceRequestId");
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(amount, "amount");
    }

    @Override
    public Quote withId(Long newId) {
        return new Quote(newId, serviceRequestId, date, amount, conditions, status);
    }
}
