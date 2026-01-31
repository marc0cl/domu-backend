package com.domu.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HousingUnitResponse(
                Long id,
                Long buildingId,
                String number,
                String tower,
                String floor,
                BigDecimal aliquotPercentage,
                BigDecimal squareMeters,
                String status,
                Long createdByUserId,
                String createdByUserName,
                LocalDateTime createdAt,
                LocalDateTime updatedAt,
                Integer residentCount) {
}
