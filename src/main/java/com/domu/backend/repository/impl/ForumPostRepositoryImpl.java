package com.domu.backend.repository.impl;

import com.domu.backend.domain.ForumPost;
import com.domu.backend.repository.ForumPostRepository;
import com.domu.backend.repository.base.AbstractJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public class ForumPostRepositoryImpl extends AbstractJpaRepository<ForumPost> implements ForumPostRepository {

    public ForumPostRepositoryImpl() {
        super(ForumPost.class);
    }
}
