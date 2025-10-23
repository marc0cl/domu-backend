package com.domu.backend.domain.vendor;

import com.domu.backend.domain.Identifiable;

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
) implements Identifiable<Invoice> {
    public Invoice {
        Objects.requireNonNull(serviceRequestId, "serviceRequestId");
        Objects.requireNonNull(number, "number");
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(netAmount, "netAmount");
        Objects.requireNonNull(totalAmount, "totalAmount");
    }

    @Override
    public Invoice withId(Long newId) {
        return new Invoice(newId, serviceRequestId, number, date, netAmount, vatAmount, totalAmount, fileUrl, status);
    }
}
