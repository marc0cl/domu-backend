package com.domu.service;

import com.domu.database.UserRepository;
import com.domu.database.MarketRepository;
import com.domu.database.HousingUnitRepository;
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

    @Inject
    public UserProfileService(UserRepository userRepository, MarketRepository marketRepository, HousingUnitRepository housingUnitRepository) {
        this.userRepository = userRepository;
        this.marketRepository = marketRepository;
        this.housingUnitRepository = housingUnitRepository;
    }

    public UserProfileResponse getProfile(Long userId, Long buildingId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ValidationException("Usuario no encontrado"));

        String unitIdentifier = user.unitId() != null 
                ? housingUnitRepository.findById(user.unitId()).map(u -> u.number() + " (" + u.tower() + ")").orElse(null)
                : "Sin unidad";

        return UserProfileResponse.builder()
                .id(user.id())
                .firstName(user.firstName())
                .lastName(user.lastName())
                .bio(user.bio())
                .avatarUrl(user.avatarBoxId()) // In real case, generate shared link
                .unitIdentifier(unitIdentifier)
                .itemsForSale(marketRepository.findAllByBuilding(buildingId, null, "AVAILABLE").stream()
                        .filter(i -> i.userId().equals(userId))
                        .toList())
                .build();
    }
}
