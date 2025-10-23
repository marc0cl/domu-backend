package com.domu.backend.controllers;

import com.domu.backend.domain.Delivery;
import com.domu.backend.domain.ParkingPermit;
import com.domu.backend.domain.ParkingSpace;
import com.domu.backend.domain.Visit;
import com.domu.backend.dto.DeliveryRequest;
import com.domu.backend.dto.ParkingPermitRequest;
import com.domu.backend.dto.ParkingSpaceRequest;
import com.domu.backend.dto.VisitRequest;
import com.domu.backend.services.AccessService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/access")
public class AccessController {

    private final AccessService accessService;

    public AccessController(AccessService accessService) {
        this.accessService = accessService;
    }

    @PostMapping("/visits")
    public Visit createVisit(@Valid @RequestBody VisitRequest request) {
        return accessService.createVisit(request);
    }

    @GetMapping("/visits")
    public List<Visit> listVisits() {
        return accessService.listVisits();
    }

    @PostMapping("/deliveries")
    public Delivery createDelivery(@Valid @RequestBody DeliveryRequest request) {
        return accessService.createDelivery(request);
    }

    @GetMapping("/deliveries")
    public List<Delivery> listDeliveries() {
        return accessService.listDeliveries();
    }

    @PostMapping("/parking-spaces")
    public ParkingSpace createParkingSpace(@Valid @RequestBody ParkingSpaceRequest request) {
        return accessService.createParkingSpace(request);
    }

    @GetMapping("/parking-spaces")
    public List<ParkingSpace> listParkingSpaces() {
        return accessService.listParkingSpaces();
    }

    @PostMapping("/parking-permits")
    public ParkingPermit createParkingPermit(@Valid @RequestBody ParkingPermitRequest request) {
        return accessService.createParkingPermit(request);
    }

    @GetMapping("/parking-permits")
    public List<ParkingPermit> listParkingPermits() {
        return accessService.listParkingPermits();
    }
}
