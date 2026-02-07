package com.domu.service;

import com.domu.database.UserRepository;
import com.domu.database.MarketRepository;
import com.domu.database.HousingUnitRepository;
import com.domu.database.ChatRepository;
import com.domu.dto.MarketItemResponse;
import com.domu.dto.UserProfileResponse;
import com.domu.domain.core.User;
import com.domu.web.UserMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.List;
import java.util.Optional;

@Singleton
public class UserProfileService {

    private final UserRepository userRepository;
    private final MarketRepository marketRepository;
    private final HousingUnitRepository housingUnitRepository;
    private final ChatRepository chatRepository;
    private final GcsStorageService gcsStorageService;
    private final MarketService marketService;

    @Inject
    public UserProfileService(UserRepository userRepository, MarketRepository marketRepository,
                              HousingUnitRepository housingUnitRepository, ChatRepository chatRepository,
                              GcsStorageService gcsStorageService, MarketService marketService) {
        this.userRepository = userRepository;
        this.marketRepository = marketRepository;
        this.housingUnitRepository = housingUnitRepository;
        this.chatRepository = chatRepository;
        this.gcsStorageService = gcsStorageService;
        this.marketService = marketService;
    }

    public UserProfileResponse getProfile(Long userId, Long buildingId, Long requesterId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ValidationException("Usuario no encontrado"));

        String unitIdentifier = user.unitId() != null 
                ? housingUnitRepository.findById(user.unitId()).map(u -> u.number() + " (" + u.tower() + ")").orElse(null)
                : "Sin unidad";

        Long activeChatRoomId = null;
        if (requesterId != null && !requesterId.equals(userId)) {
             activeChatRoomId = chatRepository.findDirectChatRoom(requesterId, userId).orElse(null);
        }

        // Get items with resolved URLs via MarketService
        List<MarketItemResponse> itemsForSale = marketService.listItems(buildingId, null, "AVAILABLE").stream()
                .filter(i -> i.userId().equals(userId))
                .toList();

        return UserProfileResponse.builder()
                .id(user.id())
                .firstName(user.firstName())
                .lastName(user.lastName())
                .bio(user.bio())
                .avatarUrl(UserMapper.resolveUrl(user.avatarBoxId(), gcsStorageService))
                .unitIdentifier(unitIdentifier)
                .activeChatRoomId(activeChatRoomId)
                .itemsForSale(itemsForSale)
                .build();
    }
}