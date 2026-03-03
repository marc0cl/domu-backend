package com.domu.domain.vendor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

public record Quotation(
        Long id,
        Long serviceOrderId,
        Long providerId,
        BigDecimal amount,
        String description,
        LocalDate validUntil,
        String fileId,
        String status,
        LocalDateTime createdAt
) {
    public Quotation {
        Objects.requireNonNull(serviceOrderId, "serviceOrderId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(amount, "amount");
    }
}
