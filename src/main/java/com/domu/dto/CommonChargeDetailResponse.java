package com.domu.dto;

import java.math.BigDecimal;

public record CommonChargeDetailResponse(
        Long id,
        String description,
        String type,
        String origin,
        BigDecimal amount,
        Boolean prorateable,
        String receiptFileName,
        Boolean receiptAvailable
) {
}
