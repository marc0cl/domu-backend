package com.domu.dto;

import java.time.LocalDateTime;

public record NotificationResponse(
    Long id,
    Long buildingId,
    String type,
    String title,
    String message,
    String data,
    boolean isRead,
    LocalDateTime createdAt,
    LocalDateTime readAt
) {}
