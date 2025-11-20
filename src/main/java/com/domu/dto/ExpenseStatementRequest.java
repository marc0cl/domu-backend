package com.domu.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseStatementRequest(
        @NotNull Long communityId,
        @NotNull Long unitId,
        @NotNull LocalDate periodStart,
        @NotNull LocalDate periodEnd,
        @NotNull BigDecimal chargesTotal,
        BigDecimal paymentsTotal,
        @NotNull BigDecimal balanceDue,
        @NotNull LocalDate dueDate,
        String status
) {
}
