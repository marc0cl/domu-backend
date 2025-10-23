package com.domu.backend.domain.vendor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public record Quote(
        Long id,
        Long serviceRequestId,
        LocalDate date,
        BigDecimal amount,
        String conditions,
        String status
) {
    public Quote {
        Objects.requireNonNull(serviceRequestId, "serviceRequestId");
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(amount, "amount");
    }
}
