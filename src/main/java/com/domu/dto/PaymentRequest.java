package com.domu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentRequest(
        @NotNull Long statementId,
        @NotNull Long residentId,
        @NotNull BigDecimal amount,
        @NotBlank String method,
        LocalDateTime paidAt,
        String receiptUrl,
        String transactionReference
) {
}
