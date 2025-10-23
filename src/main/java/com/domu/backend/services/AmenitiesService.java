package com.domu.backend.services;

import com.domu.backend.domain.Amenity;
import com.domu.backend.domain.Community;
import com.domu.backend.domain.Reservation;
import com.domu.backend.domain.Resident;
import com.domu.backend.dto.AmenityRequest;
import com.domu.backend.dto.ReservationRequest;
import com.domu.backend.exceptions.ResourceNotFoundException;
import com.domu.backend.repository.AmenityRepository;
import com.domu.backend.repository.CommunityRepository;
import com.domu.backend.repository.ReservationRepository;
import com.domu.backend.repository.ResidentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AmenitiesService {

    private final AmenityRepository amenityRepository;
    private final CommunityRepository communityRepository;
    private final ReservationRepository reservationRepository;
    private final ResidentRepository residentRepository;

    public AmenitiesService(AmenityRepository amenityRepository,
                            CommunityRepository communityRepository,
                            ReservationRepository reservationRepository,
                            ResidentRepository residentRepository) {
        this.amenityRepository = amenityRepository;
        this.communityRepository = communityRepository;
        this.reservationRepository = reservationRepository;
        this.residentRepository = residentRepository;
    }

    public Amenity createAmenity(AmenityRequest request) {
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

    public List<Amenity> listAmenities() {
        return amenityRepository.findAll();
    }

    public Reservation createReservation(ReservationRequest request) {
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

    public List<Reservation> listReservations() {
        return reservationRepository.findAll();
    }
}
