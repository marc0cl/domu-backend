package com.domu.service;

import com.domu.database.ForumRepository;
import com.domu.dto.ForumThreadDto;
import com.domu.dto.CreateThreadRequest;
import com.domu.domain.core.User;
import com.google.inject.Inject;
import java.util.List;

public class ForumService {
    private final ForumRepository forumRepository;

    @Inject
    public ForumService(ForumRepository forumRepository) {
        this.forumRepository = forumRepository;
    }

    public List<ForumThreadDto> getThreads(Long buildingId) {
        return forumRepository.findAllByBuildingId(buildingId);
    }
    
    public void createThread(Long buildingId, User user, CreateThreadRequest request) {
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new ValidationException("El título es obligatorio");
        }
        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new ValidationException("El contenido es obligatorio");
        }
        if (request.getCategory() == null || request.getCategory().isBlank()) {
            throw new ValidationException("La categoría es obligatoria");
        }

        Long categoryId = forumRepository.findCategoryIdByName(request.getCategory())
            .orElseThrow(() -> new ValidationException("Categoría no válida: " + request.getCategory()));
        
        boolean isPinned = Boolean.TRUE.equals(request.getPinned());
        
        // Only admin/concierge can pin (Role 1 or 3)
        if (isPinned) {
             if (user.roleId() == null || (user.roleId() != 1L && user.roleId() != 3L)) {
                 throw new ValidationException("Solo administradores pueden fijar publicaciones");
             }
        }

        Long threadId = forumRepository.createThread(buildingId, user.id(), categoryId, request.getTitle(), isPinned);
        forumRepository.createPost(threadId, user.id(), request.getContent());
    }

    public void updateThread(Long threadId, User user, CreateThreadRequest request) {
        ForumThreadDto thread = forumRepository.findById(threadId)
            .orElseThrow(() -> new ValidationException("Publicación no encontrada"));

        // Permission check: Author OR Admin
        boolean isAdmin = user.roleId() != null && user.roleId() == 1L;
        if (!thread.authorId().equals(user.id()) && !isAdmin) {
            throw new ValidationException("No tienes permiso para editar esta publicación");
        }

        Long categoryId = null;
        if (request.getCategory() != null) {
            categoryId = forumRepository.findCategoryIdByName(request.getCategory())
                .orElseThrow(() -> new ValidationException("Categoría no válida: " + request.getCategory()));
        }

        forumRepository.updateThread(threadId, request.getTitle(), categoryId, request.getPinned());
        if (request.getContent() != null) {
            forumRepository.updatePostContent(threadId, request.getContent());
        }
    }

    public void deleteThread(Long threadId, User user) {
        ForumThreadDto thread = forumRepository.findById(threadId)
            .orElseThrow(() -> new ValidationException("Publicación no encontrada"));

        // Permission check: Author OR Admin
        boolean isAdmin = user.roleId() != null && user.roleId() == 1L;
        if (!thread.authorId().equals(user.id()) && !isAdmin) {
            throw new ValidationException("No tienes permiso para eliminar esta publicación");
        }

        forumRepository.deleteThread(threadId);
    }
}