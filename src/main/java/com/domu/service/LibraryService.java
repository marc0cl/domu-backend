package com.domu.service;

import com.domu.database.LibraryRepository;
import com.domu.domain.LibraryDocument;
import com.domu.domain.core.User;
import com.domu.dto.LibraryDocumentResponse;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class LibraryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LibraryService.class);
    private final LibraryRepository libraryRepository;
    private final BoxStorageService boxStorageService;

    @Inject
    public LibraryService(LibraryRepository libraryRepository, BoxStorageService boxStorageService) {
        this.libraryRepository = libraryRepository;
        this.boxStorageService = boxStorageService;
    }

    public List<LibraryDocumentResponse> listDocuments(Long buildingId) {
        return libraryRepository.findAllByBuildingId(buildingId).stream()
                .map(doc -> {
                    String url = resolveFileUrl(doc.fileUrl());
                    return LibraryDocumentResponse.from(new LibraryDocument(
                            doc.id(),
                            doc.buildingId(),
                            doc.name(),
                            doc.category(),
                            doc.fileName(),
                            url,
                            doc.size(),
                            doc.uploadDate(),
                            doc.uploadedBy()
                    ));
                })
                .collect(Collectors.toList());
    }

    public LibraryDocumentResponse uploadDocument(Long buildingId, User user, String name, String category, String fileName, byte[] content, String contentType) {
        LOGGER.info("Uploading document: {} to building: {} by user: {}", fileName, buildingId, user.id());
        if (!"application/pdf".equals(contentType)) {
            throw new ValidationException("Solo se permiten archivos PDF");
        }
        if (content.length > 30 * 1024 * 1024) {
            throw new ValidationException("El archivo excede el límite de 30MB");
        }

        String objectPath = boxStorageService.libraryDocPath(buildingId, fileName);
        String fileId = boxStorageService.upload(objectPath, content, contentType);
        String proxyUrl = boxStorageService.resolveUrl(fileId);

        LibraryDocument doc = new LibraryDocument(
                null,
                buildingId,
                name,
                category,
                fileName,
                fileId,
                (long) content.length,
                null,
                user.id()
        );

        Long id = libraryRepository.save(doc);

        return LibraryDocumentResponse.from(new LibraryDocument(
                id,
                doc.buildingId(),
                doc.name(),
                doc.category(),
                doc.fileName(),
                proxyUrl,
                doc.size(),
                doc.uploadDate(),
                doc.uploadedBy()
        ));
    }

    public void deleteDocument(Long buildingId, Long docId, User user) {
        var doc = libraryRepository.findById(docId)
                .orElseThrow(() -> new ValidationException("Documento no encontrado"));

        if (!doc.buildingId().equals(buildingId)) {
            throw new ValidationException("El documento no pertenece a este edificio");
        }

        String fileUrl = doc.fileUrl();
        libraryRepository.delete(docId);
        if (fileUrl != null && !fileUrl.isBlank() && !fileUrl.startsWith("http")) {
            boxStorageService.delete(fileUrl);
        }
    }

    private String resolveFileUrl(String stored) {
        if (stored == null || stored.isBlank()) return null;
        if (stored.startsWith("http")) return stored;
        return boxStorageService.resolveUrl(stored);
    }
}
