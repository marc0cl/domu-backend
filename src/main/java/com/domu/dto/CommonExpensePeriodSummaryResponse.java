package com.domu.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record CommonExpensePeriodSummaryResponse(
        Long periodId,
        Integer year,
        Integer month,
        LocalDate dueDate,
        BigDecimal reserveAmount,
        BigDecimal totalAmount,
        String status,
        Integer chargesCount,
        Integer correctionsCount,
        LocalDateTime lastCorrectionAt
) {
}
