package com.domu.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class CommonPaymentRequest {
    private BigDecimal amount;
    private String paymentMethod;
    private String reference;
    private String receiptText;
}

