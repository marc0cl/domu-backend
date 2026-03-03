package com.domu.dto;

import java.util.List;

public record UserProfileResponse(
    Long id,
    String firstName,
    String lastName,
    String bio,
    String avatarUrl,
    String unitIdentifier,
    Long activeChatRoomId,
    List<MarketItemResponse> itemsForSale
) {}
