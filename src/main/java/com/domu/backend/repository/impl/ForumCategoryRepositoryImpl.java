package com.domu.backend.repository.impl;

import com.domu.backend.domain.ForumCategory;
import com.domu.backend.repository.ForumCategoryRepository;
import com.domu.backend.repository.base.AbstractJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public class ForumCategoryRepositoryImpl extends AbstractJpaRepository<ForumCategory> implements ForumCategoryRepository {

    public ForumCategoryRepositoryImpl() {
        super(ForumCategory.class);
    }
}
