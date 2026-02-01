package com.domu.dto;

import lombok.Builder;
import java.time.LocalDateTime;

@Builder
public record MarketItemResponse(
    Long id,
    Long userId,
    String sellerName,
    String sellerPhotoUrl,
    Long categoryId,
    String categoryName,
    String title,
    String description,
    Double price,
    String originalPriceLink,
    String status,
    String mainImageUrl,
    LocalDateTime createdAt
) {}
