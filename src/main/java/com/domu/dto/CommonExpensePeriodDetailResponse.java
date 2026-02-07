package com.domu.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CommonExpensePeriodDetailResponse(
        Long periodId,
        Integer year,
        Integer month,
        LocalDate dueDate,
        BigDecimal reserveAmount,
        BigDecimal totalAmount,
        String status,
        BigDecimal unitTotal,
        BigDecimal unitPaid,
        BigDecimal unitPending,
        String buildingName,
        String buildingAddress,
        String buildingCommune,
        String buildingCity,
        String unitLabel,
        List<CommonChargeDetailResponse> charges,
        List<CommonPaymentDetailResponse> payments,
        List<CommonExpenseRevisionResponse> revisions
) {
}
