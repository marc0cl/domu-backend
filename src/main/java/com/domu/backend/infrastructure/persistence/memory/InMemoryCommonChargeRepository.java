package com.domu.backend.infrastructure.persistence.memory;

import com.domu.backend.domain.finance.CommonCharge;
import com.domu.backend.infrastructure.persistence.repository.CommonChargeRepository;

public class InMemoryCommonChargeRepository extends InMemoryCrudRepository<CommonCharge> implements CommonChargeRepository {
}
