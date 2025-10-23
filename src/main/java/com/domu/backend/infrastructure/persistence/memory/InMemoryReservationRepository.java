package com.domu.backend.infrastructure.persistence.memory;

import com.domu.backend.domain.facility.Reservation;
import com.domu.backend.infrastructure.persistence.repository.ReservationRepository;

public class InMemoryReservationRepository extends InMemoryCrudRepository<Reservation> implements ReservationRepository {
}
