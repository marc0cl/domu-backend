package com.domu.backend.services;

import com.domu.backend.domain.Building;
import com.domu.backend.domain.Community;
import com.domu.backend.domain.Unit;
import com.domu.backend.dto.BuildingRequest;
import com.domu.backend.dto.CommunityRequest;
import com.domu.backend.dto.UnitRequest;
import com.domu.backend.exceptions.ResourceNotFoundException;
import com.domu.backend.repository.BuildingRepository;
import com.domu.backend.repository.CommunityRepository;
import com.domu.backend.repository.UnitRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommunityService {

    private final CommunityRepository communityRepository;
    private final BuildingRepository buildingRepository;
    private final UnitRepository unitRepository;

    public CommunityService(CommunityRepository communityRepository,
                            BuildingRepository buildingRepository,
                            UnitRepository unitRepository) {
        this.communityRepository = communityRepository;
        this.buildingRepository = buildingRepository;
        this.unitRepository = unitRepository;
    }

    public Community createCommunity(CommunityRequest request) {
        Community community = new Community();
        community.setName(request.name());
        community.setAddress(request.address());
        community.setCity(request.city());
        community.setCountry(request.country());
        community.setMaxUnits(request.maxUnits());
        return communityRepository.save(community);
    }

    public List<Community> listCommunities() {
        return communityRepository.findAll();
    }

    public Building createBuilding(BuildingRequest request) {
        Community community = communityRepository.findById(request.communityId())
                .orElseThrow(() -> new ResourceNotFoundException("Community not found"));
        Building building = new Building();
        building.setCommunity(community);
        building.setName(request.name());
        building.setDescription(request.description());
        return buildingRepository.save(building);
    }

    public List<Building> listBuildings() {
        return buildingRepository.findAll();
    }

    public Unit createUnit(UnitRequest request) {
        Building building = buildingRepository.findById(request.buildingId())
                .orElseThrow(() -> new ResourceNotFoundException("Building not found"));
        Unit unit = new Unit();
        unit.setBuilding(building);
        unit.setNumber(request.number());
        unit.setFloor(request.floor());
        unit.setAreaM2(request.areaM2());
        if (request.status() != null) {
            unit.setStatus(request.status());
        }
        return unitRepository.save(unit);
    }

    public List<Unit> listUnits() {
        return unitRepository.findAll();
    }
}
