package com.domu.backend.controllers;

import com.domu.backend.domain.Resident;
import com.domu.backend.dto.ResidentRequest;
import com.domu.backend.dto.RoleAssignmentRequest;
import com.domu.backend.services.ResidentService;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/residents")
public class ResidentController {

    private final ResidentService residentService;

    public ResidentController(ResidentService residentService) {
        this.residentService = residentService;
    }

    @PostMapping
    public Resident createResident(@Valid @RequestBody ResidentRequest request) {
        return residentService.createResident(request);
    }

    @GetMapping
    public List<Resident> listResidents() {
        return residentService.listResidents();
    }

    @PostMapping("/assign-roles")
    @Transactional
    public Resident assignRoles(@Valid @RequestBody RoleAssignmentRequest request) {
        return residentService.assignRoles(request);
    }
}
