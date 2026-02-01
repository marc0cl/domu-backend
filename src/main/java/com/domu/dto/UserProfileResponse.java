package com.domu.dto;

import lombok.Builder;
import java.util.List;

@Builder
public record UserProfileResponse(
    Long id,
    String firstName,
    String lastName,
    String bio,
    String avatarUrl,
    String unitIdentifier,
    List<MarketItemResponse> itemsForSale
) {}
