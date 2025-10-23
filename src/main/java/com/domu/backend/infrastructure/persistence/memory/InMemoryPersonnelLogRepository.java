package com.domu.backend.infrastructure.persistence.memory;

import com.domu.backend.domain.staff.PersonnelLog;
import com.domu.backend.infrastructure.persistence.repository.PersonnelLogRepository;

public class InMemoryPersonnelLogRepository extends InMemoryCrudRepository<PersonnelLog> implements PersonnelLogRepository {
}
