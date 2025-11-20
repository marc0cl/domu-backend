package com.domu.domain.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public record CommonPayment(
        Long id,
        Long unitId,
        Long chargeId,
        Long userId,
        LocalDate issuedAt,
        BigDecimal amount,
        String paymentMethod,
        String reference,
        String status
) {
    public CommonPayment {
        Objects.requireNonNull(unitId, "unitId");
        Objects.requireNonNull(chargeId, "chargeId");
        Objects.requireNonNull(issuedAt, "issuedAt");
        Objects.requireNonNull(amount, "amount");
    }
}
