package com.domu.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PollResponse(
        Long id,
        Long buildingId,
        Long createdBy,
        String title,
        String description,
        String status,
        LocalDateTime closesAt,
        LocalDateTime createdAt,
        LocalDateTime closedAt,
        List<PollOptionResponse> options,
        boolean voted,
        Long selectedOptionId,
        Long totalVotes) {
}
