package com.domu.backend.repository.impl;

import com.domu.backend.domain.Community;
import com.domu.backend.repository.CommunityRepository;
import com.domu.backend.repository.base.AbstractJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public class CommunityRepositoryImpl extends AbstractJpaRepository<Community> implements CommunityRepository {

    public CommunityRepositoryImpl() {
        super(Community.class);
    }
}
