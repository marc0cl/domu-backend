package com.domu.dto;

import java.util.List;

public record VisitListResponse(
        List<VisitResponse> upcoming,
        List<VisitResponse> past
) {
}

