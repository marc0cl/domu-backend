package com.domu.dto;

import java.time.LocalDateTime;

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
    String senderUnitNumber,
    String senderPrivacyPhoto,
    String receiverUnitNumber,
    String receiverPrivacyPhoto,
    LocalDateTime createdAt
) {}
