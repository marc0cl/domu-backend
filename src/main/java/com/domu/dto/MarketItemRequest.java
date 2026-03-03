package com.domu.dto;

public class MarketItemRequest {
    private String title;
    private String description;
    private Double price;
    private String originalPriceLink;
    private Long categoryId;

    public MarketItemRequest() {}

    public MarketItemRequest(String title, String description, Double price, String originalPriceLink, Long categoryId) {
        this.title = title;
        this.description = description;
        this.price = price;
        this.originalPriceLink = originalPriceLink;
        this.categoryId = categoryId;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public String getOriginalPriceLink() { return originalPriceLink; }
    public void setOriginalPriceLink(String originalPriceLink) { this.originalPriceLink = originalPriceLink; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
}
