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
    private final GcsStorageService gcsStorageService;

    @Inject
    public LibraryService(LibraryRepository libraryRepository, GcsStorageService gcsStorageService) {
        this.libraryRepository = libraryRepository;
        this.gcsStorageService = gcsStorageService;
    }

    public List<LibraryDocumentResponse> listDocuments(Long buildingId) {
        return libraryRepository.findAllByBuildingId(buildingId).stream()
                .map(doc -> {
                    // Update signed URL if needed
                    String signedUrl = gcsStorageService.signedUrl(gcsStorageService.extractObjectPath(doc.fileUrl()));
                    return LibraryDocumentResponse.from(new LibraryDocument(
                            doc.id(),
                            doc.buildingId(),
                            doc.name(),
                            doc.category(),
                            doc.fileName(),
                            signedUrl,
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

        String objectPath = gcsStorageService.libraryDocPath(buildingId, fileName);
        String fileUrl = gcsStorageService.upload(objectPath, content, contentType);

        LibraryDocument doc = new LibraryDocument(
                null,
                buildingId,
                name,
                category,
                fileName,
                fileUrl,
                (long) content.length,
                null,
                user.id()
        );

        Long id = libraryRepository.save(doc);
        String finalSignedUrl = gcsStorageService.signedUrl(objectPath);
        
        return LibraryDocumentResponse.from(new LibraryDocument(
                id,
                doc.buildingId(),
                doc.name(),
                doc.category(),
                doc.fileName(),
                finalSignedUrl,
                doc.size(),
                doc.uploadDate(),
                doc.uploadedBy()
        ));
    }

    public void deleteDocument(Long buildingId, Long docId, User user) {
        // Only admin can delete (checked in WebServer)
        var doc = libraryRepository.findById(docId)
                .orElseThrow(() -> new ValidationException("Documento no encontrado"));
        
        if (!doc.buildingId().equals(buildingId)) {
            throw new ValidationException("El documento no pertenece a este edificio");
        }

        libraryRepository.delete(docId);
        // We could also delete from GCS, but maybe keep for history as requested "evitando la perdida de informacion"
        // gcsStorageService.delete(gcsStorageService.extractObjectPath(doc.fileUrl()));
    }
}
