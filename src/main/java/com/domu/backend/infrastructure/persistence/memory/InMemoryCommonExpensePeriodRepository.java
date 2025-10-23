package com.domu.backend.infrastructure.persistence.memory;

import com.domu.backend.domain.finance.CommonExpensePeriod;
import com.domu.backend.infrastructure.persistence.repository.CommonExpensePeriodRepository;

public class InMemoryCommonExpensePeriodRepository extends InMemoryCrudRepository<CommonExpensePeriod> implements CommonExpensePeriodRepository {
}
