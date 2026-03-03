package com.domu.dto;

public record NotificationPreferenceUpdateRequest(
    String notificationType,
    boolean inAppEnabled
) {}
