package com.domu.dto;

import java.util.List;

public record IncidentListResponse(
        List<IncidentResponse> reported,
        List<IncidentResponse> inProgress,
        List<IncidentResponse> closed
) {
}

