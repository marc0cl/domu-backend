package com.domu.backend.infrastructure.persistence.memory;

import com.domu.backend.domain.access.VisitAuthorization;
import com.domu.backend.infrastructure.persistence.repository.VisitAuthorizationRepository;

public class InMemoryVisitAuthorizationRepository extends InMemoryCrudRepository<VisitAuthorization> implements VisitAuthorizationRepository {
}
