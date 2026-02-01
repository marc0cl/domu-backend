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
        return repository.insertRequest(senderId, receiverId, buildingId, itemId, message);
    }

    public void updateRequestStatus(Long requestId, String status) {
        ChatRequestResponse req = repository.findById(requestId)
                .orElseThrow(() -> new ValidationException("Solicitud no encontrada"));

        repository.updateStatus(requestId, status);

        if ("APPROVED".equals(status)) {
            // Create the actual chat room
            Long roomId = chatRepository.createRoom(req.buildingId(), req.itemId());
            chatRepository.addParticipant(roomId, req.senderId());
            chatRepository.addParticipant(roomId, req.receiverId());
            
            // Optionally insert the initial message
            if (req.initialMessage() != null && !req.initialMessage().isBlank()) {
                chatRepository.insertMessage(roomId, req.senderId(), req.initialMessage(), "TEXT", null);
            }
        }
    }
}
