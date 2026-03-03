package com.domu.dto;

import java.math.BigDecimal;

public class CommonPaymentRequest {
    private BigDecimal amount;
    private String paymentMethod;
    private String reference;
    private String receiptText;

    public CommonPaymentRequest() {}

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getReceiptText() { return receiptText; }
    public void setReceiptText(String receiptText) { this.receiptText = receiptText; }
}
