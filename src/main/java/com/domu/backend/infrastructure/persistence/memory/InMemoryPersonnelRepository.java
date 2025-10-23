package com.domu.backend.infrastructure.persistence.memory;

import com.domu.backend.domain.staff.Personnel;
import com.domu.backend.infrastructure.persistence.repository.PersonnelRepository;

public class InMemoryPersonnelRepository extends InMemoryCrudRepository<Personnel> implements PersonnelRepository {
}
