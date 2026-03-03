package com.domu.dto;

public class VoteRequest {
    private Long optionId;

    public VoteRequest() {}

    public Long getOptionId() { return optionId; }
    public void setOptionId(Long optionId) { this.optionId = optionId; }
}
