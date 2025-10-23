package com.domu.backend.infrastructure.persistence.memory;

import com.domu.backend.domain.core.Building;
import com.domu.backend.infrastructure.persistence.repository.BuildingRepository;

public class InMemoryBuildingRepository extends InMemoryCrudRepository<Building> implements BuildingRepository {
}
