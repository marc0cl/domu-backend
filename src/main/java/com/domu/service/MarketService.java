package com.domu.service;

import com.domu.database.MarketRepository;
import com.domu.dto.MarketItemRequest;
import com.domu.dto.MarketItemResponse;
import com.domu.service.ValidationException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.List;

@Singleton
public class MarketService {

    private final MarketRepository repository;
    private final MarketplaceStorageService storageService;

    @Inject
    public MarketService(MarketRepository repository, MarketplaceStorageService storageService) {
        this.repository = repository;
        this.storageService = storageService;
    }

    public List<MarketItemResponse> listItems(Long buildingId, Long categoryId, String status) {
        return repository.findAllByBuilding(buildingId, categoryId, status);
    }

    public MarketItemResponse createItem(Long userId, Long buildingId, MarketItemRequest request, String fileName, byte[] imageContent) {
        Long itemId = repository.insertItem(
                userId, 
                buildingId, 
                request.getCategoryId(), 
                request.getTitle(), 
                request.getDescription(), 
                request.getPrice(), 
                request.getOriginalPriceLink()
        );

        String boxFileId = null;
        if (imageContent != null && imageContent.length > 0) {
            boxFileId = storageService.uploadMarketImage(buildingId, itemId, fileName, imageContent);
            repository.updateBoxMetadata(itemId, null, boxFileId);
        }

        return repository.findAllByBuilding(buildingId, null, null).stream()
                .filter(i -> i.id().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ValidationException("Error recuperando el item creado"));
    }
}