package com.domu.dto;

import lombok.Builder;
import java.time.LocalDateTime;
import java.util.List;

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
    List<ImageInfo> images,
    List<String> imageUrls,
    LocalDateTime createdAt
) {
    @Builder
    public record ImageInfo(
        Long id,
        String url,
        boolean isMain
    ) {}
}
