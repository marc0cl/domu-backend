package com.domu.backend.repository.impl;

import com.domu.backend.domain.ForumThread;
import com.domu.backend.repository.ForumThreadRepository;
import com.domu.backend.repository.base.AbstractJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public class ForumThreadRepositoryImpl extends AbstractJpaRepository<ForumThread> implements ForumThreadRepository {

    public ForumThreadRepositoryImpl() {
        super(ForumThread.class);
    }
}
