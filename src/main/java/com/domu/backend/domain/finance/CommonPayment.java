package com.domu.backend.domain.finance;

import com.domu.backend.domain.Identifiable;

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
) implements Identifiable<CommonPayment> {
    public CommonPayment {
        Objects.requireNonNull(unitId, "unitId");
        Objects.requireNonNull(chargeId, "chargeId");
        Objects.requireNonNull(issuedAt, "issuedAt");
        Objects.requireNonNull(amount, "amount");
    }

    @Override
    public CommonPayment withId(Long newId) {
        return new CommonPayment(newId, unitId, chargeId, userId, issuedAt, amount, paymentMethod, reference, status);
    }
}
