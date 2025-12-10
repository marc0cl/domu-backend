package com.domu.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CommonExpensePeriodResponse(
        Long periodId,
        Long buildingId,
        int year,
        int month,
        LocalDate dueDate,
        BigDecimal reserveAmount,
        BigDecimal totalAmount,
        String status,
        int chargesCreated
) {
}

