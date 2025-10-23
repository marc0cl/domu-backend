package com.domu.backend.infrastructure.persistence.memory;

import com.domu.backend.domain.staff.Shift;
import com.domu.backend.infrastructure.persistence.repository.ShiftRepository;

public class InMemoryShiftRepository extends InMemoryCrudRepository<Shift> implements ShiftRepository {
}
