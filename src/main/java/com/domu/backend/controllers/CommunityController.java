package com.domu.backend.controllers;

import com.domu.backend.domain.Building;
import com.domu.backend.domain.Community;
import com.domu.backend.domain.Unit;
import com.domu.backend.dto.BuildingRequest;
import com.domu.backend.dto.CommunityRequest;
import com.domu.backend.dto.UnitRequest;
import com.domu.backend.services.CommunityService;
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

    private final CommunityService communityService;

    public CommunityController(CommunityService communityService) {
        this.communityService = communityService;
    }

    @PostMapping
    public Community createCommunity(@Valid @RequestBody CommunityRequest request) {
        return communityService.createCommunity(request);
    }

    @GetMapping
    public List<Community> listCommunities() {
        return communityService.listCommunities();
    }

    @PostMapping("/buildings")
    public Building createBuilding(@Valid @RequestBody BuildingRequest request) {
        return communityService.createBuilding(request);
    }

    @GetMapping("/buildings")
    public List<Building> listBuildings() {
        return communityService.listBuildings();
    }

    @PostMapping("/units")
    public Unit createUnit(@Valid @RequestBody UnitRequest request) {
        return communityService.createUnit(request);
    }

    @GetMapping("/units")
    public List<Unit> listUnits() {
        return communityService.listUnits();
    }
}
