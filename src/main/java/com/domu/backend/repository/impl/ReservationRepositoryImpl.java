package com.domu.backend.repository.impl;

import com.domu.backend.domain.Reservation;
import com.domu.backend.repository.ReservationRepository;
import com.domu.backend.repository.base.AbstractJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public class ReservationRepositoryImpl extends AbstractJpaRepository<Reservation> implements ReservationRepository {

    public ReservationRepositoryImpl() {
        super(Reservation.class);
    }
}
