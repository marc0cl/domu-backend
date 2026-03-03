package com.domu.dto;

import java.math.BigDecimal;

public class CreateCommonChargeRequest {
    private Long unitId;
    private String description;
    private BigDecimal amount;
    private String type;
    private String origin;
    private Boolean prorateable;
    private String receiptText;

    public CreateCommonChargeRequest() {}

    public Long getUnitId() { return unitId; }
    public void setUnitId(Long unitId) { this.unitId = unitId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public Boolean getProrateable() { return prorateable; }
    public void setProrateable(Boolean prorateable) { this.prorateable = prorateable; }

    public String getReceiptText() { return receiptText; }
    public void setReceiptText(String receiptText) { this.receiptText = receiptText; }
}
