package com.domu.domain.finance;

import java.math.BigDecimal;
import java.util.Objects;

public record CommonCharge(
        Long id,
        Long periodId,
        Long unitId,
        String description,
        BigDecimal amount,
        String type,
        Boolean prorateable,
        String payerType,
        String receiptText
) {
    public CommonCharge {
        Objects.requireNonNull(periodId, "periodId");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(amount, "amount");
    }
}
