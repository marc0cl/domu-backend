package com.domu.backend.controllers;

import com.domu.backend.domain.Amenity;
import com.domu.backend.domain.Reservation;
import com.domu.backend.dto.AmenityRequest;
import com.domu.backend.dto.ReservationRequest;
import com.domu.backend.services.AmenitiesService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/amenities")
public class AmenitiesController {

    private final AmenitiesService amenitiesService;

    public AmenitiesController(AmenitiesService amenitiesService) {
        this.amenitiesService = amenitiesService;
    }

    @PostMapping
    public Amenity createAmenity(@Valid @RequestBody AmenityRequest request) {
        return amenitiesService.createAmenity(request);
    }

    @GetMapping
    public List<Amenity> listAmenities() {
        return amenitiesService.listAmenities();
    }

    @PostMapping("/reservations")
    public Reservation createReservation(@Valid @RequestBody ReservationRequest request) {
        return amenitiesService.createReservation(request);
    }

    @GetMapping("/reservations")
    public List<Reservation> listReservations() {
        return amenitiesService.listReservations();
    }
}
