package com.domu.domain.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public record CommonExpensePeriod(
        Long id,
        Long buildingId,
        Integer year,
        Integer month,
        LocalDate generatedAt,
        LocalDate dueDate,
        BigDecimal reserveAmount,
        BigDecimal totalAmount,
        String status
) {
    public CommonExpensePeriod {
        Objects.requireNonNull(buildingId, "buildingId");
        Objects.requireNonNull(generatedAt, "generatedAt");
        Objects.requireNonNull(dueDate, "dueDate");
        Objects.requireNonNull(reserveAmount, "reserveAmount");
        Objects.requireNonNull(totalAmount, "totalAmount");
    }
}
