package com.domu.service;

import com.domu.database.ChatRepository;
import com.domu.dto.ChatMessageResponse;
import com.domu.dto.ChatRoomResponse;
import com.domu.service.ValidationException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.List;

@Singleton
public class ChatService {

    private final ChatRepository repository;
    private final MarketplaceStorageService storageService;

    @Inject
    public ChatService(ChatRepository repository, MarketplaceStorageService storageService) {
        this.repository = repository;
        this.storageService = storageService;
    }

    public List<ChatRoomResponse> getMyRooms(Long userId, Long buildingId) {
        return repository.findRoomsByUser(userId, buildingId);
    }

    public List<ChatMessageResponse> getMessages(Long roomId, int limit) {
        return repository.findMessagesByRoom(roomId, limit);
    }

    public Long startConversation(Long buildingId, Long buyerId, Long sellerId, Long itemId) {
        Long roomId = repository.createRoom(buildingId, itemId);
        repository.addParticipant(roomId, buyerId);
        repository.addParticipant(roomId, sellerId);
        return roomId;
    }

    public ChatMessageResponse saveMessage(Long roomId, Long senderId, String content, String type, String fileName, byte[] audioContent) {
        String boxFileId = null;
        if (audioContent != null && audioContent.length > 0) {
            boxFileId = storageService.uploadChatAudio(0L, roomId, fileName, audioContent);
        }
        
        Long messageId = repository.insertMessage(roomId, senderId, content, type, boxFileId);
        return repository.findMessagesByRoom(roomId, 10).stream()
                .filter(m -> m.id().equals(messageId))
                .findFirst()
                .orElseThrow(() -> new ValidationException("Error guardando mensaje"));
    }
}