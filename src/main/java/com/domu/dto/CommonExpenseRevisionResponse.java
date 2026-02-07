package com.domu.dto;

import java.time.LocalDateTime;

public record CommonExpenseRevisionResponse(
        Long id,
        String action,
        String note,
        Long createdByUserId,
        LocalDateTime createdAt
) {
}
