package com.domu.dto;

public record UnitSummaryResponse(
                Long unitId,
                Long buildingId,
                String number,
                String tower,
                String floor) {
}
