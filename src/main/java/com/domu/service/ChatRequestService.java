package com.domu.service;

import com.domu.database.ChatRequestRepository;
import com.domu.database.ChatRepository;
import com.domu.database.BuildingRepository;
import com.domu.database.MarketRepository;
import com.domu.database.UserBuildingRepository;
import com.domu.database.UserRepository;
import com.domu.dto.ChatRequestResponse;
import com.domu.web.ChatWebSocketHandler;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.javalin.http.UnauthorizedResponse;

import java.util.List;
import java.util.Objects;

@Singleton
public class ChatRequestService {

    private final ChatRequestRepository repository;
    private final ChatRepository chatRepository;
    private final UserBuildingRepository userBuildingRepository;
    private final UserRepository userRepository;
    private final BuildingRepository buildingRepository;
    private final MarketRepository marketRepository;
    private final ChatWebSocketHandler wsHandler;

    @Inject
    public ChatRequestService(ChatRequestRepository repository,
            ChatRepository chatRepository,
            UserBuildingRepository userBuildingRepository,
            UserRepository userRepository,
            BuildingRepository buildingRepository,
            MarketRepository marketRepository,
            ChatWebSocketHandler wsHandler) {
        this.repository = repository;
        this.chatRepository = chatRepository;
        this.userBuildingRepository = userBuildingRepository;
        this.userRepository = userRepository;
        this.buildingRepository = buildingRepository;
        this.marketRepository = marketRepository;
        this.wsHandler = wsHandler;
    }

    public List<ChatRequestResponse> getPendingRequests(Long userId, Long buildingId) {
        if (buildingId == null) {
            throw new ValidationException("Debes seleccionar un edificio");
        }
        return repository.findPendingByReceiver(userId, buildingId);
    }

    public Long createRequest(Long senderId, Long receiverId, Long buildingId, Long itemId, String message) {
        if (buildingId == null) {
            throw new ValidationException("Debes seleccionar un edificio");
        }
        if (Objects.equals(senderId, receiverId)) {
            throw new ValidationException("No puedes enviarte una solicitud a ti mismo");
        }
        if (!userHasAccessToBuilding(senderId, buildingId)
                || !userHasAccessToBuilding(receiverId, buildingId)) {
            throw new ValidationException("Ambos usuarios deben pertenecer al mismo edificio");
        }
        if (itemId != null && !marketRepository.itemBelongsToBuilding(itemId, buildingId)) {
            throw new ValidationException("El producto no pertenece al edificio seleccionado");
        }

        // Prevent duplicate requests between the same two users
        if (repository.existsBetweenUsers(senderId, receiverId, buildingId)) {
            throw new ValidationException("Ya existe una solicitud de chat con este vecino.");
        }
        // Also check if they already share a chat room
        if (chatRepository.findDirectChatRoom(senderId, receiverId, buildingId).isPresent()) {
            throw new ValidationException("Ya tienes una conversación activa con este vecino.");
        }
        return repository.insertRequest(senderId, receiverId, buildingId, itemId, message);
    }

    public void updateRequestStatus(Long requestId, String status, Long actingUserId, Long selectedBuildingId) {
        if (selectedBuildingId == null) {
            throw new ValidationException("Debes seleccionar un edificio");
        }
        ChatRequestResponse req = repository.findById(requestId)
                .orElseThrow(() -> new ValidationException("Solicitud no encontrada"));
        if (!Objects.equals(req.buildingId(), selectedBuildingId)) {
            throw new UnauthorizedResponse("No tienes acceso a esta solicitud");
        }
        if (!Objects.equals(req.receiverId(), actingUserId)) {
            throw new UnauthorizedResponse("Solo el receptor puede responder esta solicitud");
        }
        if (!"PENDING".equalsIgnoreCase(req.status())) {
            throw new ValidationException("La solicitud ya fue procesada");
        }
        String normalizedStatus = normalizeStatus(status);

        repository.updateStatus(requestId, normalizedStatus);

        if ("APPROVED".equals(normalizedStatus)) {
            // Check if a room already exists between these users
            Long roomId = chatRepository.findDirectChatRoom(req.senderId(), req.receiverId(), req.buildingId())
                    .orElseGet(() -> {
                        Long newRoomId = chatRepository.createRoom(req.buildingId(), req.itemId());
                        chatRepository.addParticipant(newRoomId, req.senderId());
                        chatRepository.addParticipant(newRoomId, req.receiverId());
                        return newRoomId;
                    });

            // Insert the initial message if present
            if (req.initialMessage() != null && !req.initialMessage().isBlank()) {
                chatRepository.insertMessage(roomId, req.senderId(), req.initialMessage(), "TEXT", null);
            }
        }
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new ValidationException("status es obligatorio");
        }
        String normalized = status.trim().toUpperCase();
        if (!"APPROVED".equals(normalized) && !"REJECTED".equals(normalized)) {
            throw new ValidationException("status inválido");
        }
        return normalized;
    }

    private boolean userHasAccessToBuilding(Long userId, Long buildingId) {
        if (userId == null || buildingId == null) {
            return false;
        }
        if (userBuildingRepository.userHasAccessToBuilding(userId, buildingId)) {
            return true;
        }
        return userRepository.findById(userId)
                .map(user -> {
                    if (user.unitId() == null) {
                        return false;
                    }
                    Long unitBuildingId = buildingRepository.findBuildingIdByUnitId(user.unitId());
                    return buildingId.equals(unitBuildingId);
                })
                .orElse(false);
    }
}
