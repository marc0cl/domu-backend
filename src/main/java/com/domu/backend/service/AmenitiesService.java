package com.domu.backend.service;

import com.domu.backend.domain.facility.CommonArea;
import com.domu.backend.domain.facility.Reservation;
import com.domu.backend.infrastructure.persistence.repository.CommonAreaRepository;
import com.domu.backend.infrastructure.persistence.repository.ReservationRepository;

import java.util.List;

public class AmenitiesService {

    private final CommonAreaRepository commonAreaRepository;
    private final ReservationRepository reservationRepository;

    public AmenitiesService(CommonAreaRepository commonAreaRepository,
                            ReservationRepository reservationRepository) {
        this.commonAreaRepository = commonAreaRepository;
        this.reservationRepository = reservationRepository;
    }

    public CommonArea registerCommonArea(CommonArea commonArea) {
        return commonAreaRepository.save(commonArea);
    }

    public List<CommonArea> listCommonAreas() {
        return commonAreaRepository.findAll();
    }

    public Reservation createReservation(Reservation reservation) {
        commonAreaRepository.findById(reservation.commonAreaId())
                .orElseThrow(() -> new ResourceNotFoundException("Common area not found"));
        return reservationRepository.save(reservation);
    }

    public List<Reservation> listReservations() {
        return reservationRepository.findAll();
    }
}
