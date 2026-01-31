package com.domu.dto;

import java.math.BigDecimal;

public class AmenityRequest {
    private Long buildingId;
    private String name;
    private String description;
    private Integer maxCapacity;
    private BigDecimal costPerSlot;
    private String rules;
    private String imageUrl;
    private String status;

    public AmenityRequest() {
    }

    public Long getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(Long buildingId) {
        this.buildingId = buildingId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(Integer maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public BigDecimal getCostPerSlot() {
        return costPerSlot;
    }

    public void setCostPerSlot(BigDecimal costPerSlot) {
        this.costPerSlot = costPerSlot;
    }

    public String getRules() {
        return rules;
    }

    public void setRules(String rules) {
        this.rules = rules;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
