package com.domu.dto;

import java.time.LocalDateTime;

public class ParcelRequest {
    private Long unitId;
    private String sender;
    private String description;
    private LocalDateTime receivedAt;

    public ParcelRequest() {}

    public Long getUnitId() { return unitId; }
    public void setUnitId(Long unitId) { this.unitId = unitId; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getReceivedAt() { return receivedAt; }
    public void setReceivedAt(LocalDateTime receivedAt) { this.receivedAt = receivedAt; }
}
