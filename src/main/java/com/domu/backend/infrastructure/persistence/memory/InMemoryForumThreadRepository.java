package com.domu.backend.infrastructure.persistence.memory;

import com.domu.backend.domain.community.ForumThread;
import com.domu.backend.infrastructure.persistence.repository.ForumThreadRepository;

public class InMemoryForumThreadRepository extends InMemoryCrudRepository<ForumThread> implements ForumThreadRepository {
}
