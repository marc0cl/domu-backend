package com.domu.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ChatRoomResponse(
    Long id,
    Long buildingId,
    Long itemId,
    String itemTitle,
    String itemImageUrl,
    List<UserSummary> participants,
    ChatMessageResponse lastMessage,
    LocalDateTime createdAt,
    LocalDateTime lastMessageAt
) {
    public record UserSummary(
        Long id,
        String name,
        String photoUrl,
        Boolean isTyping
    ) {}
}
