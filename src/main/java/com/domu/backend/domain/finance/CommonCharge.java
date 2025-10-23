package com.domu.backend.domain.finance;

import com.domu.backend.domain.Identifiable;

import java.math.BigDecimal;
import java.util.Objects;

public record CommonCharge(
        Long id,
        Long periodId,
        String description,
        BigDecimal amount,
        String type,
        Boolean prorateable
) implements Identifiable<CommonCharge> {
    public CommonCharge {
        Objects.requireNonNull(periodId, "periodId");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(amount, "amount");
    }

    @Override
    public CommonCharge withId(Long newId) {
        return new CommonCharge(newId, periodId, description, amount, type, prorateable);
    }
}
