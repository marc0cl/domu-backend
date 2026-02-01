package com.domu.service;

import com.domu.database.MarketRepository;
import com.domu.dto.MarketItemRequest;
import com.domu.dto.MarketItemResponse;
import com.domu.service.ValidationException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.List;
import java.util.Map;

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

    public MarketItemResponse getItem(Long itemId, Long buildingId) {
        return repository.findAllByBuilding(buildingId, null, null).stream()
                .filter(i -> i.id().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ValidationException("Producto no encontrado"));
    }

    public void updateItem(Long itemId, Long userId, MarketItemRequest request, List<ImageContent> newImages, List<String> deletedImageUrls) {
        // Actualizar datos básicos
        repository.updateItem(itemId, userId, request.getCategoryId(), request.getTitle(), request.getDescription(), request.getPrice());

        // Eliminar fotos marcadas
        if (deletedImageUrls != null) {
            for (String url : deletedImageUrls) {
                repository.deleteImage(itemId, url);
            }
        }

        // Subir fotos nuevas
        if (newImages != null && !newImages.isEmpty()) {
            for (ImageContent img : newImages) {
                String directUrl = storageService.uploadMarketImage(0L, itemId, img.name(), img.content()); // buildingId 0 por simplicidad en update
                repository.insertImage(itemId, directUrl, "N/A", false);
            }
        }

        // Asegurar que haya una imagen principal si la original fue borrada
        List<String> remainingImages = repository.findAllByBuilding(0L, null, null).stream()
                .filter(i -> i.id().equals(itemId))
                .findFirst()
                .map(MarketItemResponse::imageUrls)
                .orElse(List.of());
        
        if (!remainingImages.isEmpty()) {
            repository.updateBoxMetadata(itemId, null, remainingImages.get(0));
        }
    }

    public void deleteItem(Long itemId, Long userId) {
        repository.deleteItem(itemId, userId);
    }

    public MarketItemResponse createItem(Long userId, Long buildingId, MarketItemRequest request, List<ImageContent> images) {
        Long itemId = repository.insertItem(
                userId, 
                buildingId, 
                request.getCategoryId(), 
                request.getTitle(), 
                request.getDescription(), 
                request.getPrice(), 
                request.getOriginalPriceLink()
        );

        String mainImageUrl = null;
        for (int i = 0; i < images.size(); i++) {
            ImageContent img = images.get(i);
            String directUrl = storageService.uploadMarketImage(buildingId, itemId, img.name(), img.content());
            
            boolean isMain = (i == 0);
            if (isMain) mainImageUrl = directUrl;
            
            repository.insertImage(itemId, directUrl, "N/A", isMain);
        }

        if (mainImageUrl != null) {
            repository.updateBoxMetadata(itemId, null, mainImageUrl);
        }

        return repository.findAllByBuilding(buildingId, null, null).stream()
                .filter(i -> i.id().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ValidationException("Error recuperando el item creado"));
    }

    public record ImageContent(String name, byte[] content) {}
}
