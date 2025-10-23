package com.domu.backend.infrastructure.persistence.memory;

import com.domu.backend.domain.core.Community;
import com.domu.backend.infrastructure.persistence.repository.CommunityRepository;

public class InMemoryCommunityRepository extends InMemoryCrudRepository<Community> implements CommunityRepository {
}
