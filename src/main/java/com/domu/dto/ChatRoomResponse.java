package com.domu.dto;

import lombok.Builder;
import java.time.LocalDateTime;
import java.util.List;

@Builder
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
    @Builder
    public record UserSummary(
        Long id,
        String name,
        String photoUrl,
        Boolean isTyping
    ) {}
}
