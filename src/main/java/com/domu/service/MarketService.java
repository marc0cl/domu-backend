package com.domu.service;

import com.domu.database.MarketRepository;
import com.domu.dto.MarketItemRequest;
import com.domu.dto.MarketItemResponse;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.List;

@Singleton
public class MarketService {

    private final MarketRepository repository;
    private final MarketplaceStorageService storageService;
    private final GcsStorageService gcs;

    @Inject
    public MarketService(MarketRepository repository, MarketplaceStorageService storageService, GcsStorageService gcs) {
        this.repository = repository;
        this.storageService = storageService;
        this.gcs = gcs;
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

        // Delete images by ID (also removes from GCS)
        if (deletedImageIds != null) {
            for (String idStr : deletedImageIds) {
                try {
                    Long imageId = Long.parseLong(idStr);
                    String objectPath = repository.getImagePath(imageId, itemId);
                    if (objectPath != null && !objectPath.startsWith("http")) {
                        gcs.delete(objectPath);
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

        return repository.findAllByBuilding(buildingId, null, null).stream()
                .filter(i -> i.id().equals(itemId))
                .findFirst()
                .map(this::resolveUrls)
                .orElseThrow(() -> new ValidationException("Error recuperando el item creado"));
    }

    /**
     * Resolves stored GCS object paths to fresh signed URLs for the frontend.
     */
    private MarketItemResponse resolveUrls(MarketItemResponse item) {
        List<MarketItemResponse.ImageInfo> resolvedImages = item.images() != null
                ? item.images().stream().map(img -> MarketItemResponse.ImageInfo.builder()
                        .id(img.id())
                        .url(resolveUrl(img.url()))
                        .isMain(img.isMain())
                        .build()).toList()
                : List.of();

        return MarketItemResponse.builder()
                .id(item.id())
                .userId(item.userId())
                .sellerName(item.sellerName())
                .sellerPhotoUrl(resolveUrl(item.sellerPhotoUrl()))
                .categoryId(item.categoryId())
                .categoryName(item.categoryName())
                .title(item.title())
                .description(item.description())
                .price(item.price())
                .originalPriceLink(item.originalPriceLink())
                .status(item.status())
                .mainImageUrl(resolveUrl(item.mainImageUrl()))
                .images(resolvedImages)
                .imageUrls(resolvedImages.stream().map(MarketItemResponse.ImageInfo::url).toList())
                .createdAt(item.createdAt())
                .build();
    }

    private String resolveUrl(String stored) {
        if (stored == null || stored.isBlank()) return null;
        if (!stored.startsWith("http")) {
            return gcs.signedUrl(stored);
        }
        if (stored.contains("storage.googleapis.com")) {
            String prefix = "storage.googleapis.com/";
            int idx = stored.indexOf(prefix);
            if (idx >= 0) {
                String rest = stored.substring(idx + prefix.length());
                int slash = rest.indexOf('/');
                if (slash > 0) {
                    String path = rest.substring(slash + 1);
                    int q = path.indexOf('?');
                    if (q > 0) path = path.substring(0, q);
                    return gcs.signedUrl(path);
                }
            }
        }
        return stored;
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
