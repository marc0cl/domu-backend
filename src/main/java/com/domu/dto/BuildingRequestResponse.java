package com.domu.dto;

import java.time.LocalDateTime;

public record BuildingRequestResponse(
        Long requestId,
        String status,
        Long buildingId,
        LocalDateTime createdAt,
        String reviewNotes,
        String boxFolderId,
        String boxFileId,
        String boxFileName
) {
}

