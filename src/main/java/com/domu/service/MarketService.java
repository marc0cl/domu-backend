package com.domu.service;

import com.domu.database.MarketRepository;
import com.domu.domain.NotificationType;
import com.domu.dto.MarketItemRequest;
import com.domu.dto.MarketItemResponse;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.List;

@Singleton
public class MarketService {

    private final MarketRepository repository;
    private final MarketplaceStorageService storageService;
    private final BoxStorageService box;
    private final NotificationService notificationService;

    @Inject
    public MarketService(MarketRepository repository, MarketplaceStorageService storageService, BoxStorageService box,
                         NotificationService notificationService) {
        this.repository = repository;
        this.storageService = storageService;
        this.box = box;
        this.notificationService = notificationService;
    }

    public List<MarketItemResponse> listItems(Long buildingId, Long categoryId, String status) {
        if (buildingId == null) {
            throw new ValidationException("Debes seleccionar un edificio");
        }
        return repository.findAllByBuilding(buildingId, categoryId, status).stream()
                .map(this::resolveUrls)
                .toList();
    }

    public MarketItemResponse getItem(Long itemId, Long buildingId) {
        if (buildingId == null) {
            throw new ValidationException("Debes seleccionar un edificio");
        }
        return repository.findAllByBuilding(buildingId, null, null).stream()
                .filter(i -> i.id().equals(itemId))
                .findFirst()
                .map(this::resolveUrls)
                .orElseThrow(() -> new ValidationException("Producto no encontrado"));
    }

    public void updateItem(Long itemId, Long userId, Long buildingId, MarketItemRequest request, List<ImageContent> newImages, List<String> deletedImageIds) {
        MarketRepository.MarketItemAccessRow item = repository.findAccessRowById(itemId)
                .orElseThrow(() -> new ValidationException("Producto no encontrado"));
        ensureOwnerAndBuilding(item, userId, buildingId);

        repository.updateItem(itemId, userId, request.getCategoryId(), request.getTitle(), request.getDescription(), request.getPrice());

        // Delete images by ID (also removes from Box)
        if (deletedImageIds != null) {
            for (String idStr : deletedImageIds) {
                try {
                    Long imageId = Long.parseLong(idStr);
                    String fileId = repository.getImagePath(imageId, itemId);
                    if (fileId != null && !fileId.startsWith("http")) {
                        box.delete(fileId);
                    }
                    repository.deleteImageById(imageId, itemId);
                } catch (NumberFormatException e) {
                    // Legacy: try by URL for backward compatibility
                    repository.deleteImage(itemId, idStr);
                }
            }
        }

        // Upload new images
        if (newImages != null && !newImages.isEmpty()) {
            for (ImageContent img : newImages) {
                String path = storageService.uploadMarketImage(buildingId, itemId, img.name(), img.content());
                repository.insertImage(itemId, path, path, false);
            }
        }

        // Reassign main image if needed
        repository.reassignMainImage(itemId);
    }

    public void deleteItem(Long itemId, Long userId, Long buildingId) {
        MarketRepository.MarketItemAccessRow item = repository.findAccessRowById(itemId)
                .orElseThrow(() -> new ValidationException("Producto no encontrado"));
        ensureOwnerAndBuilding(item, userId, buildingId);
        repository.deleteItem(itemId, userId);
    }

    public MarketItemResponse createItem(Long userId, Long buildingId, MarketItemRequest request, List<ImageContent> images) {
        if (buildingId == null) {
            throw new ValidationException("Debes seleccionar un edificio");
        }
        Long itemId = repository.insertItem(
                userId, 
                buildingId, 
                request.getCategoryId(), 
                request.getTitle(), 
                request.getDescription(), 
                request.getPrice(), 
                request.getOriginalPriceLink()
        );

        List<ImageContent> safeImages = images != null ? images : List.of();
        String mainImagePath = null;
        for (int i = 0; i < safeImages.size(); i++) {
            ImageContent img = safeImages.get(i);
            String path = storageService.uploadMarketImage(buildingId, itemId, img.name(), img.content());
            
            boolean isMain = (i == 0);
            if (isMain) mainImagePath = path;
            
            repository.insertImage(itemId, path, path, isMain);
        }

        if (mainImagePath != null) {
            repository.updateBoxMetadata(itemId, null, mainImagePath);
        }

        MarketItemResponse created = repository.findAllByBuilding(buildingId, null, null).stream()
                .filter(i -> i.id().equals(itemId))
                .findFirst()
                .map(this::resolveUrls)
                .orElseThrow(() -> new ValidationException("Error recuperando el item creado"));

        notificationService.notifyAllBuildingUsers(buildingId,
                NotificationType.MARKET_ITEM_CREATED,
                "Nuevo articulo: " + request.getTitle(),
                "Se ha publicado un nuevo articulo en el marketplace.",
                "{\"itemId\":" + itemId + "}",
                userId);

        return created;
    }

    /**
     * Resolves stored Box file IDs to proxy URLs for the frontend.
     */
    private MarketItemResponse resolveUrls(MarketItemResponse item) {
        List<MarketItemResponse.ImageInfo> resolvedImages = item.images() != null
                ? item.images().stream().map(img -> new MarketItemResponse.ImageInfo(
                        img.id(),
                        resolveUrl(img.url()),
                        img.isMain()
                )).toList()
                : List.of();

        return new MarketItemResponse(
                item.id(),
                item.userId(),
                item.sellerName(),
                resolveUrl(item.sellerPhotoUrl()),
                item.categoryId(),
                item.categoryName(),
                item.title(),
                item.description(),
                item.price(),
                item.originalPriceLink(),
                item.status(),
                resolveUrl(item.mainImageUrl()),
                resolvedImages,
                resolvedImages.stream().map(MarketItemResponse.ImageInfo::url).toList(),
                item.createdAt()
        );
    }

    private String resolveUrl(String stored) {
        if (stored == null || stored.isBlank()) return null;
        if (stored.startsWith("http")) return stored;
        return box.resolveUrl(stored);
    }

    private void ensureOwnerAndBuilding(MarketRepository.MarketItemAccessRow item, Long userId, Long buildingId) {
        if (buildingId == null) {
            throw new ValidationException("Debes seleccionar un edificio");
        }
        if (!buildingId.equals(item.buildingId())) {
            throw new ValidationException("Producto no disponible en el edificio seleccionado");
        }
        if (!userId.equals(item.userId())) {
            throw new ValidationException("No tienes permisos para modificar este producto");
        }
    }

    public record ImageContent(String name, byte[] content) {}
}
