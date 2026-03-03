package com.domu.dto;

public record ServiceOrderStatusRequest(
        String status,
        String notes
) {
}
