package com.domu.backend.service;

import com.domu.backend.domain.core.Building;
import com.domu.backend.domain.core.Community;
import com.domu.backend.domain.core.HousingUnit;
import com.domu.backend.infrastructure.persistence.repository.BuildingRepository;
import com.domu.backend.infrastructure.persistence.repository.CommunityRepository;
import com.domu.backend.infrastructure.persistence.repository.HousingUnitRepository;

import java.util.List;

public class CommunityService {

    private final CommunityRepository communityRepository;
    private final BuildingRepository buildingRepository;
    private final HousingUnitRepository housingUnitRepository;

    public CommunityService(CommunityRepository communityRepository,
                            BuildingRepository buildingRepository,
                            HousingUnitRepository housingUnitRepository) {
        this.communityRepository = communityRepository;
        this.buildingRepository = buildingRepository;
        this.housingUnitRepository = housingUnitRepository;
    }

    public Community registerCommunity(Community community) {
        return communityRepository.save(community);
    }

    public List<Community> listCommunities() {
        return communityRepository.findAll();
    }

    public Building registerBuilding(Building building) {
        return buildingRepository.save(building);
    }

    public List<Building> listBuildings() {
        return buildingRepository.findAll();
    }

    public HousingUnit registerHousingUnit(HousingUnit unit) {
        buildingRepository.findById(unit.buildingId())
                .orElseThrow(() -> new ResourceNotFoundException("Building not found"));
        return housingUnitRepository.save(unit);
    }

    public List<HousingUnit> listHousingUnits() {
        return housingUnitRepository.findAll();
    }
}
