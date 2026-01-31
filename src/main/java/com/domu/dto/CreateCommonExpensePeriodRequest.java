package com.domu.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.Data;

@Data
public class CreateCommonExpensePeriodRequest {
    private Long buildingId;
    private Integer year;
    private Integer month;
    private LocalDate dueDate;
    private BigDecimal reserveAmount;
    private List<CreateCommonChargeRequest> charges;
    private String note;
}
