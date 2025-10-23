package com.domu.backend.domain.finance;

import com.domu.backend.domain.Identifiable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public record CommonExpensePeriod(
        Long id,
        Long buildingId,
        int year,
        int month,
        LocalDate generatedAt,
        LocalDate dueDate,
        BigDecimal totalAmount,
        String status
) implements Identifiable<CommonExpensePeriod> {
    public CommonExpensePeriod {
        Objects.requireNonNull(buildingId, "buildingId");
        Objects.requireNonNull(generatedAt, "generatedAt");
        Objects.requireNonNull(dueDate, "dueDate");
        Objects.requireNonNull(totalAmount, "totalAmount");
    }

    @Override
    public CommonExpensePeriod withId(Long newId) {
        return new CommonExpensePeriod(newId, buildingId, year, month, generatedAt, dueDate, totalAmount, status);
    }
}
