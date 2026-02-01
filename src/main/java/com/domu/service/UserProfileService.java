package com.domu.service;

import com.domu.database.UserRepository;
import com.domu.database.MarketRepository;
import com.domu.database.HousingUnitRepository;
import com.domu.database.ChatRepository;
import com.domu.dto.UserProfileResponse;
import com.domu.domain.core.User;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.Optional;

@Singleton
public class UserProfileService {

    private final UserRepository userRepository;
    private final MarketRepository marketRepository;
    private final HousingUnitRepository housingUnitRepository;
    private final ChatRepository chatRepository;

    @Inject
    public UserProfileService(UserRepository userRepository, MarketRepository marketRepository, HousingUnitRepository housingUnitRepository, ChatRepository chatRepository) {
        this.userRepository = userRepository;
        this.marketRepository = marketRepository;
        this.housingUnitRepository = housingUnitRepository;
        this.chatRepository = chatRepository;
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

        return UserProfileResponse.builder()
                .id(user.id())
                .firstName(user.firstName())
                .lastName(user.lastName())
                .bio(user.bio())
                .avatarUrl(user.avatarBoxId()) // In real case, generate shared link
                .unitIdentifier(unitIdentifier)
                .activeChatRoomId(activeChatRoomId)
                .itemsForSale(marketRepository.findAllByBuilding(buildingId, null, "AVAILABLE").stream()
                        .filter(i -> i.userId().equals(userId))
                        .toList())
                .build();
    }
}