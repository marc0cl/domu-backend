package com.domu.backend.infrastructure.persistence.memory;

import com.domu.backend.domain.community.ForumCategory;
import com.domu.backend.infrastructure.persistence.repository.ForumCategoryRepository;

public class InMemoryForumCategoryRepository extends InMemoryCrudRepository<ForumCategory> implements ForumCategoryRepository {
}
