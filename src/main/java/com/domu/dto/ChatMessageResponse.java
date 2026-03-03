package com.domu.dto;

import java.time.LocalDateTime;

public record ChatMessageResponse(
    Long id,
    Long roomId,
    Long senderId,
    String senderName,
    String content,
    String type,
    String boxFileId,
    LocalDateTime createdAt,
    LocalDateTime readAt
) {}
