package com.domu.backend.infrastructure.persistence.memory;

import com.domu.backend.domain.access.ParkingSpace;
import com.domu.backend.infrastructure.persistence.repository.ParkingSpaceRepository;

public class InMemoryParkingSpaceRepository extends InMemoryCrudRepository<ParkingSpace> implements ParkingSpaceRepository {
}
