package com.domu.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketItemRequest {
    private String title;
    private String description;
    private Double price;
    private String originalPriceLink;
    private Long categoryId;
}
