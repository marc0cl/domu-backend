package com.domu.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class CreateCommonChargeRequest {
    private Long unitId;
    private String description;
    private BigDecimal amount;
    private String type;
    private Boolean prorateable;
    private String receiptText;
}

