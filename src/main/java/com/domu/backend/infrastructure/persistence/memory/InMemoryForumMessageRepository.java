package com.domu.backend.infrastructure.persistence.memory;

import com.domu.backend.domain.community.ForumMessage;
import com.domu.backend.infrastructure.persistence.repository.ForumMessageRepository;

public class InMemoryForumMessageRepository extends InMemoryCrudRepository<ForumMessage> implements ForumMessageRepository {
}
