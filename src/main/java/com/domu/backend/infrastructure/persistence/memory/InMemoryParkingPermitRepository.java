package com.domu.backend.infrastructure.persistence.memory;

import com.domu.backend.domain.access.ParkingPermit;
import com.domu.backend.infrastructure.persistence.repository.ParkingPermitRepository;

public class InMemoryParkingPermitRepository extends InMemoryCrudRepository<ParkingPermit> implements ParkingPermitRepository {
}
