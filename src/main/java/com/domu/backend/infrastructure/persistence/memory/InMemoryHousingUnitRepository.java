package com.domu.backend.infrastructure.persistence.memory;

import com.domu.backend.domain.core.HousingUnit;
import com.domu.backend.infrastructure.persistence.repository.HousingUnitRepository;

public class InMemoryHousingUnitRepository extends InMemoryCrudRepository<HousingUnit> implements HousingUnitRepository {
}
