package com.domu.backend.controllers;

import com.domu.backend.domain.Amenity;
import com.domu.backend.domain.Community;
import com.domu.backend.domain.Reservation;
import com.domu.backend.domain.Resident;
import com.domu.backend.dto.AmenityRequest;
import com.domu.backend.dto.ReservationRequest;
import com.domu.backend.repository.AmenityRepository;
import com.domu.backend.repository.CommunityRepository;
import com.domu.backend.repository.ReservationRepository;
import com.domu.backend.repository.ResidentRepository;
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

    private final AmenityRepository amenityRepository;
    private final CommunityRepository communityRepository;
    private final ReservationRepository reservationRepository;
    private final ResidentRepository residentRepository;

    public AmenitiesController(AmenityRepository amenityRepository,
                               CommunityRepository communityRepository,
                               ReservationRepository reservationRepository,
                               ResidentRepository residentRepository) {
        this.amenityRepository = amenityRepository;
        this.communityRepository = communityRepository;
        this.reservationRepository = reservationRepository;
        this.residentRepository = residentRepository;
    }

    @PostMapping
    public Amenity createAmenity(@Valid @RequestBody AmenityRequest request) {
        Community community = communityRepository.findById(request.communityId())
                .orElseThrow(() -> new ResourceNotFoundException("Community not found"));
        Amenity amenity = new Amenity();
        amenity.setCommunity(community);
        amenity.setName(request.name());
        amenity.setDescription(request.description());
        amenity.setCapacity(request.capacity());
        amenity.setMaxDurationMinutes(request.maxDurationMinutes());
        if (request.allowIfInDebt() != null) {
            amenity.setAllowIfInDebt(request.allowIfInDebt());
        }
        return amenityRepository.save(amenity);
    }

    @GetMapping
    public List<Amenity> listAmenities() {
        return amenityRepository.findAll();
    }

    @PostMapping("/reservations")
    public Reservation createReservation(@Valid @RequestBody ReservationRequest request) {
        Amenity amenity = amenityRepository.findById(request.amenityId())
                .orElseThrow(() -> new ResourceNotFoundException("Amenity not found"));
        Resident resident = residentRepository.findById(request.residentId())
                .orElseThrow(() -> new ResourceNotFoundException("Resident not found"));
        Reservation reservation = new Reservation();
        reservation.setAmenity(amenity);
        reservation.setResident(resident);
        reservation.setStartTime(request.startTime());
        reservation.setEndTime(request.endTime());
        reservation.setAttendeesCount(request.attendeesCount());
        if (request.status() != null) {
            reservation.setStatus(request.status());
        }
        return reservationRepository.save(reservation);
    }

    @GetMapping("/reservations")
    public List<Reservation> listReservations() {
        return reservationRepository.findAll();
    }
}
