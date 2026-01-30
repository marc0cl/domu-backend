package com.domu.dto;

public class LinkResidentToUnitRequest {
    private Long userId;

    public LinkResidentToUnitRequest() {
    }

    public LinkResidentToUnitRequest(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
