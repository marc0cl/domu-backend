package com.domu.dto;

import java.math.BigDecimal;

public class SimulatedPaymentRequest {
    private BigDecimal amount;
    private String paymentMethod;

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}
