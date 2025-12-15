package com.domu.service;

import com.domu.database.VisitContactRepository;
import com.domu.domain.core.User;
import com.domu.dto.CreateVisitRequest;
import com.domu.dto.VisitContactRequest;
import com.domu.dto.VisitContactResponse;
import com.domu.dto.VisitFromContactRequest;
import com.google.inject.Inject;
import io.javalin.http.UnauthorizedResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class VisitContactService {

    private static final int DEFAULT_LIMIT = 20;

    private final VisitContactRepository visitContactRepository;
    private final VisitService visitService;

    @Inject
    public VisitContactService(
            VisitContactRepository visitContactRepository,
            VisitService visitService
    ) {
        this.visitContactRepository = visitContactRepository;
        this.visitService = visitService;
    }

    public VisitContactResponse create(User user, VisitContactRequest request) {
        ensureUser(user);
        if (request == null || request.getVisitorName() == null || request.getVisitorName().isBlank()) {
            throw new ValidationException("visitorName es obligatorio");
        }
        String normalizedName = request.getVisitorName().trim();
        String normalizedDocument = normalizeDocument(request.getVisitorDocument());
        Long unitId = request.getUnitId();

        VisitContactRepository.ContactRow saved = visitContactRepository.insert(new VisitContactRepository.ContactRow(
                null,
                user.id(),
                normalizedName,
                normalizedDocument,
                unitId,
                request.getAlias(),
                LocalDateTime.now(),
                LocalDateTime.now()
        ));
        return toResponse(saved);
    }

    public List<VisitContactResponse> list(User user, String search, Integer limit) {
        ensureUser(user);
        int resolvedLimit = (limit != null && limit > 0 && limit <= 50) ? limit : DEFAULT_LIMIT;
        return visitContactRepository.list(user.id(), search, resolvedLimit)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public void delete(User user, Long contactId) {
        ensureUser(user);
        visitContactRepository.delete(contactId, user.id());
    }

    public com.domu.dto.VisitResponse registerFromContact(User user, Long contactId, VisitFromContactRequest request) {
        ensureUser(user);
        VisitContactRepository.ContactRow contact = visitContactRepository.findById(contactId, user.id())
                .orElseThrow(() -> new ValidationException("Contacto no encontrado"));

        CreateVisitRequest visitRequest = new CreateVisitRequest();
        visitRequest.setVisitorName(contact.visitorName());
        visitRequest.setVisitorDocument(contact.visitorDocument());
        visitRequest.setVisitorType("VISIT");

        if (request != null) {
            visitRequest.setValidFrom(request.getValidFrom());
            visitRequest.setValidUntil(request.getValidUntil());
            visitRequest.setValidForMinutes(request.getValidForMinutes());
            if (request.getUnitId() != null) {
                visitRequest.setUnitId(request.getUnitId());
            }
            if (request.getVisitorType() != null) {
                visitRequest.setVisitorType(request.getVisitorType());
            }
            visitRequest.setCompany(request.getCompany());
        }

        if (visitRequest.getUnitId() == null && contact.unitId() != null) {
            visitRequest.setUnitId(contact.unitId());
        }

        return visitService.createVisit(user, visitRequest);
    }

    private VisitContactResponse toResponse(VisitContactRepository.ContactRow row) {
        return new VisitContactResponse(
                row.id(),
                row.visitorName(),
                row.visitorDocument(),
                row.unitId(),
                row.alias(),
                row.createdAt(),
                row.updatedAt()
        );
    }

    private void ensureUser(User user) {
        if (user == null) {
            throw new UnauthorizedResponse("Debes iniciar sesión para gestionar contactos de visita");
        }
    }

    private String normalizeDocument(String document) {
        if (document == null) {
            return null;
        }
        return document.replace(".", "").replace(" ", "").toUpperCase();
    }
}

