package com.domu.backend.infrastructure.persistence.memory;

import com.domu.backend.domain.facility.CommonArea;
import com.domu.backend.infrastructure.persistence.repository.CommonAreaRepository;

public class InMemoryCommonAreaRepository extends InMemoryCrudRepository<CommonArea> implements CommonAreaRepository {
}
