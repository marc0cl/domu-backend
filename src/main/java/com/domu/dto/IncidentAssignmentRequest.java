package com.domu.dto;

public class IncidentAssignmentRequest {
    private Long assignedToUserId;

    public IncidentAssignmentRequest() {}

    public IncidentAssignmentRequest(Long assignedToUserId) {
        this.assignedToUserId = assignedToUserId;
    }

    public Long getAssignedToUserId() {
        return assignedToUserId;
    }

    public void setAssignedToUserId(Long assignedToUserId) {
        this.assignedToUserId = assignedToUserId;
    }
}
