package com.domu.dto;

import lombok.Builder;
import java.time.LocalDateTime;

@Builder
public record ChatRequestResponse(
    Long id,
    Long senderId,
    String senderName,
    Long receiverId,
    Long buildingId,
    Long itemId,
    String itemTitle,
    String status,
    String initialMessage,
    LocalDateTime createdAt
) {}
