package com.domu.backend.controllers;

import com.domu.backend.domain.Building;
import com.domu.backend.domain.Community;
import com.domu.backend.domain.Unit;
import com.domu.backend.dto.BuildingRequest;
import com.domu.backend.dto.CommunityRequest;
import com.domu.backend.dto.UnitRequest;
import com.domu.backend.repository.BuildingRepository;
import com.domu.backend.repository.CommunityRepository;
import com.domu.backend.repository.UnitRepository;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/communities")
public class CommunityController {

    private final CommunityRepository communityRepository;
    private final BuildingRepository buildingRepository;
    private final UnitRepository unitRepository;

    public CommunityController(CommunityRepository communityRepository,
                               BuildingRepository buildingRepository,
                               UnitRepository unitRepository) {
        this.communityRepository = communityRepository;
        this.buildingRepository = buildingRepository;
        this.unitRepository = unitRepository;
    }

    @PostMapping
    public Community createCommunity(@Valid @RequestBody CommunityRequest request) {
        Community community = new Community();
        community.setName(request.name());
        community.setAddress(request.address());
        community.setCity(request.city());
        community.setCountry(request.country());
        community.setMaxUnits(request.maxUnits());
        return communityRepository.save(community);
    }

    @GetMapping
    public List<Community> listCommunities() {
        return communityRepository.findAll();
    }

    @PostMapping("/buildings")
    public Building createBuilding(@Valid @RequestBody BuildingRequest request) {
        Community community = communityRepository.findById(request.communityId())
                .orElseThrow(() -> new ResourceNotFoundException("Community not found"));
        Building building = new Building();
        building.setCommunity(community);
        building.setName(request.name());
        building.setDescription(request.description());
        return buildingRepository.save(building);
    }

    @GetMapping("/buildings")
    public List<Building> listBuildings() {
        return buildingRepository.findAll();
    }

    @PostMapping("/units")
    public Unit createUnit(@Valid @RequestBody UnitRequest request) {
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

    @GetMapping("/units")
    public List<Unit> listUnits() {
        return unitRepository.findAll();
    }
}
