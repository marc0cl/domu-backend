package com.domu.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CommonPaymentResponse(
        Long chargeId,
        BigDecimal pending,
        List<PaymentLine> payments
) {
    public record PaymentLine(
            BigDecimal amount,
            String description,
            LocalDate issuedAt,
            String receiptText
    ) {
    }
}

