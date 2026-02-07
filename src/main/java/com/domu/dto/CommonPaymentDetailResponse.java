package com.domu.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CommonPaymentDetailResponse(
        Long id,
        Long chargeId,
        String chargeDescription,
        BigDecimal amount,
        String paymentMethod,
        String reference,
        String status,
        LocalDate issuedAt
) {
}
