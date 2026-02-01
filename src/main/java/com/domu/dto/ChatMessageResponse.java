package com.domu.dto;

import lombok.Builder;
import java.time.LocalDateTime;

@Builder
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
