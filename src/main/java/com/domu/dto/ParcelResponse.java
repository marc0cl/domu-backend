package com.domu.dto;

import java.time.LocalDateTime;

public record ParcelResponse(
                Long id,
                Long buildingId,
                Long unitId,
                String unitNumber,
                String unitTower,
                String unitFloor,
                Long receivedByUserId,
                Long retrievedByUserId,
                String sender,
                String description,
                String status,
                LocalDateTime receivedAt,
                LocalDateTime retrievedAt,
                LocalDateTime createdAt,
                LocalDateTime updatedAt) {
}
