package com.domu.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record QuotationRequest(
        BigDecimal amount,
        String description,
        LocalDate validUntil,
        String fileId
) {
}
