package com.domu.domain.vendor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public record Invoice(
        Long id,
        Long serviceRequestId,
        String number,
        LocalDate date,
        BigDecimal netAmount,
        BigDecimal vatAmount,
        BigDecimal totalAmount,
        String fileUrl,
        String status
) {
    public Invoice {
        Objects.requireNonNull(serviceRequestId, "serviceRequestId");
        Objects.requireNonNull(number, "number");
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(netAmount, "netAmount");
        Objects.requireNonNull(totalAmount, "totalAmount");
    }
}
