package com.domu.service;

import com.domu.database.ChatRepository;
import com.domu.dto.ChatMessageResponse;
import com.domu.dto.ChatRoomResponse;
import com.domu.service.ValidationException;
import com.domu.web.UserMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.List;

@Singleton
public class ChatService {

    private final ChatRepository repository;
    private final MarketplaceStorageService storageService;
    private final GcsStorageService gcsStorageService;

    @Inject
    public ChatService(ChatRepository repository, MarketplaceStorageService storageService, GcsStorageService gcsStorageService) {
        this.repository = repository;
        this.storageService = storageService;
        this.gcsStorageService = gcsStorageService;
    }

    public List<ChatRoomResponse> getMyRooms(Long userId, Long buildingId) {
        List<ChatRoomResponse> rooms = repository.findRoomsByUser(userId, buildingId);
        return rooms.stream().map(this::resolveParticipantPhotos).toList();
    }

    private ChatRoomResponse resolveParticipantPhotos(ChatRoomResponse room) {
        if (room.participants() == null || room.participants().isEmpty()) {
            return room;
        }
        List<ChatRoomResponse.UserSummary> resolved = room.participants().stream()
                .map(p -> ChatRoomResponse.UserSummary.builder()
                        .id(p.id())
                        .name(p.name())
                        .photoUrl(UserMapper.resolveUrl(p.photoUrl(), gcsStorageService))
                        .isTyping(p.isTyping())
                        .build())
                .toList();
        return ChatRoomResponse.builder()
                .id(room.id())
                .buildingId(room.buildingId())
                .itemId(room.itemId())
                .itemTitle(room.itemTitle())
                .itemImageUrl(room.itemImageUrl())
                .participants(resolved)
                .lastMessage(room.lastMessage())
                .createdAt(room.createdAt())
                .lastMessageAt(room.lastMessageAt())
                .build();
    }

    public List<ChatMessageResponse> getMessages(Long roomId, int limit) {
        return repository.findMessagesByRoom(roomId, limit);
    }

    public List<Long> getParticipantIds(Long roomId) {
        return repository.getParticipantIds(roomId);
    }

    public Long startConversation(Long buildingId, Long buyerId, Long sellerId, Long itemId) {
        Long roomId = repository.createRoom(buildingId, itemId);
        repository.addParticipant(roomId, buyerId);
        repository.addParticipant(roomId, sellerId);
        return roomId;
    }

    public void hideRoom(Long roomId, Long userId) {
        // Verify user is a participant
        List<Long> participants = repository.getParticipantIds(roomId);
        if (!participants.contains(userId)) {
            throw new ValidationException("No eres participante de esta sala");
        }
        repository.hideRoom(roomId, userId);
    }

    public void unhideRoomForParticipants(Long roomId, Long excludeUserId) {
        List<Long> participants = repository.getParticipantIds(roomId);
        for (Long userId : participants) {
            if (!userId.equals(excludeUserId)) {
                repository.unhideRoom(roomId, userId);
            }
        }
    }

    public ChatMessageResponse saveMessage(Long roomId, Long senderId, String content, String type, String fileName, byte[] audioContent) {
        String boxFileId = null;
        if (audioContent != null && audioContent.length > 0) {
            boxFileId = storageService.uploadChatAudio(0L, roomId, fileName, audioContent);
        }
        
        Long messageId = repository.insertMessage(roomId, senderId, content, type, boxFileId);

        // Unhide room for other participants so they see new messages
        unhideRoomForParticipants(roomId, senderId);

        return repository.findMessagesByRoom(roomId, 10).stream()
                .filter(m -> m.id().equals(messageId))
                .findFirst()
                .orElseThrow(() -> new ValidationException("Error guardando mensaje"));
    }
}