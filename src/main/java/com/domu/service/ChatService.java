package com.domu.service;

import com.domu.database.ChatRepository;
import com.domu.database.BuildingRepository;
import com.domu.database.MarketRepository;
import com.domu.database.UserBuildingRepository;
import com.domu.database.UserRepository;
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
    private final MarketRepository marketRepository;
    private final UserBuildingRepository userBuildingRepository;
    private final UserRepository userRepository;
    private final BuildingRepository buildingRepository;
    private final MarketplaceStorageService storageService;
    private final GcsStorageService gcsStorageService;

    @Inject
    public ChatService(ChatRepository repository,
            MarketRepository marketRepository,
            UserBuildingRepository userBuildingRepository,
            UserRepository userRepository,
            BuildingRepository buildingRepository,
            MarketplaceStorageService storageService,
            GcsStorageService gcsStorageService) {
        this.repository = repository;
        this.marketRepository = marketRepository;
        this.userBuildingRepository = userBuildingRepository;
        this.userRepository = userRepository;
        this.buildingRepository = buildingRepository;
        this.storageService = storageService;
        this.gcsStorageService = gcsStorageService;
    }

    public List<ChatRoomResponse> getMyRooms(Long userId, Long buildingId) {
        if (buildingId == null) {
            throw new ValidationException("Debes seleccionar un edificio");
        }
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

    public List<ChatMessageResponse> getMessages(Long roomId, Long userId, Long buildingId, int limit) {
        ensureRoomAccess(roomId, userId, buildingId);
        return repository.findMessagesByRoom(roomId, limit);
    }

    public List<Long> getParticipantIds(Long roomId) {
        return repository.getParticipantIds(roomId);
    }

    public Long startConversation(Long buildingId, Long buyerId, Long sellerId, Long itemId) {
        if (buildingId == null) {
            throw new ValidationException("Debes seleccionar un edificio");
        }
        if (!userHasAccessToBuilding(buyerId, buildingId)
                || !userHasAccessToBuilding(sellerId, buildingId)) {
            throw new ValidationException("Ambos usuarios deben pertenecer al edificio seleccionado");
        }
        if (itemId != null && !marketRepository.itemBelongsToBuilding(itemId, buildingId)) {
            throw new ValidationException("El producto no pertenece al edificio seleccionado");
        }
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
        if (!repository.isParticipant(roomId, senderId)) {
            throw new ValidationException("No eres participante de esta sala");
        }

        String boxFileId = null;
        if (audioContent != null && audioContent.length > 0) {
            Long buildingId = repository.findRoomBuildingId(roomId)
                    .orElseThrow(() -> new ValidationException("Sala de chat no encontrada"));
            boxFileId = storageService.uploadChatAudio(buildingId, roomId, fileName, audioContent);
        }
        
        Long messageId = repository.insertMessage(roomId, senderId, content, type, boxFileId);

        // Unhide room for other participants so they see new messages
        unhideRoomForParticipants(roomId, senderId);

        return repository.findMessagesByRoom(roomId, 10).stream()
                .filter(m -> m.id().equals(messageId))
                .findFirst()
                .orElseThrow(() -> new ValidationException("Error guardando mensaje"));
    }

    private void ensureRoomAccess(Long roomId, Long userId, Long buildingId) {
        if (!repository.isParticipant(roomId, userId)) {
            throw new ValidationException("No eres participante de esta sala");
        }
        if (buildingId != null && !repository.roomBelongsToBuilding(roomId, buildingId)) {
            throw new ValidationException("La sala no pertenece al edificio seleccionado");
        }
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
