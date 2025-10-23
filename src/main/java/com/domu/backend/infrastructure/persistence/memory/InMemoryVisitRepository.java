package com.domu.backend.infrastructure.persistence.memory;

import com.domu.backend.domain.access.Visit;
import com.domu.backend.infrastructure.persistence.repository.VisitRepository;

public class InMemoryVisitRepository extends InMemoryCrudRepository<Visit> implements VisitRepository {
}
