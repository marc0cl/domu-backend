package com.domu.dto;

public record NotificationPreferenceResponse(
    String notificationType,
    String label,
    boolean inAppEnabled
) {}
