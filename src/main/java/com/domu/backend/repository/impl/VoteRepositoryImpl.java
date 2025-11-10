package com.domu.backend.repository.impl;

import com.domu.backend.domain.Vote;
import com.domu.backend.repository.VoteRepository;
import com.domu.backend.repository.base.AbstractJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public class VoteRepositoryImpl extends AbstractJpaRepository<Vote> implements VoteRepository {

    public VoteRepositoryImpl() {
        super(Vote.class);
    }
}
