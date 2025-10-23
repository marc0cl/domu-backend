package com.domu.backend.infrastructure.persistence.memory;

import com.domu.backend.domain.finance.DelinquencyRecord;
import com.domu.backend.infrastructure.persistence.repository.DelinquencyRecordRepository;

public class InMemoryDelinquencyRecordRepository extends InMemoryCrudRepository<DelinquencyRecord> implements DelinquencyRecordRepository {
}
