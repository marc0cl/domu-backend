package com.domu.dto;

import java.time.LocalDateTime;
import java.util.List;

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
    public record ImageInfo(
        Long id,
        String url,
        boolean isMain
    ) {}
}
