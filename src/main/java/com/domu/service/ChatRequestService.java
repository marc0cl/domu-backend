package com.domu.service;

import com.domu.database.ChatRequestRepository;
import com.domu.database.ChatRepository;
import com.domu.dto.ChatRequestResponse;
import com.domu.web.ChatWebSocketHandler;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.List;

@Singleton
public class ChatRequestService {

    private final ChatRequestRepository repository;
    private final ChatRepository chatRepository;
    private final ChatWebSocketHandler wsHandler;

    @Inject
    public ChatRequestService(ChatRequestRepository repository, ChatRepository chatRepository, ChatWebSocketHandler wsHandler) {
        this.repository = repository;
        this.chatRepository = chatRepository;
        this.wsHandler = wsHandler;
    }

    public List<ChatRequestResponse> getPendingRequests(Long userId) {
        return repository.findPendingByReceiver(userId);
    }

    public Long createRequest(Long senderId, Long receiverId, Long buildingId, Long itemId, String message) {
        // Prevent duplicate requests between the same two users
        if (repository.existsBetweenUsers(senderId, receiverId)) {
            throw new ValidationException("Ya existe una solicitud de chat con este vecino.");
        }
        // Also check if they already share a chat room
        if (chatRepository.findDirectChatRoom(senderId, receiverId).isPresent()) {
            throw new ValidationException("Ya tienes una conversación activa con este vecino.");
        }
        return repository.insertRequest(senderId, receiverId, buildingId, itemId, message);
    }

    public void updateRequestStatus(Long requestId, String status) {
        ChatRequestResponse req = repository.findById(requestId)
                .orElseThrow(() -> new ValidationException("Solicitud no encontrada"));

        repository.updateStatus(requestId, status);

        if ("APPROVED".equals(status)) {
            // Check if a room already exists between these users
            Long roomId = chatRepository.findDirectChatRoom(req.senderId(), req.receiverId())
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
}
