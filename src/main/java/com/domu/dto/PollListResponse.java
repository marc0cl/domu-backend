package com.domu.dto;

import java.util.List;

public record PollListResponse(
        List<PollResponse> open,
        List<PollResponse> closed) {
}
