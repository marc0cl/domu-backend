package com.domu.backend.domain.finance;

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
) {
    public CommonExpensePeriod {
        Objects.requireNonNull(buildingId, "buildingId");
        Objects.requireNonNull(generatedAt, "generatedAt");
        Objects.requireNonNull(dueDate, "dueDate");
        Objects.requireNonNull(totalAmount, "totalAmount");
    }
}
