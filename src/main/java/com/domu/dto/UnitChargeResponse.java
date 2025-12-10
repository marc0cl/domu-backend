package com.domu.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UnitChargeResponse(
        Long chargeId,
        Long periodId,
        int year,
        int month,
        String description,
        BigDecimal amount,
        BigDecimal paid,
        BigDecimal pending,
        LocalDate dueDate,
        String status,
        String type,
        String payerType,
        String receiptText
) {
}

