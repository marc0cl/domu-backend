package com.domu.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UnitCommonExpenseSummaryResponse(
        Long periodId,
        Integer year,
        Integer month,
        LocalDate dueDate,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal pendingAmount,
        String status
) {
}
