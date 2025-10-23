package com.domu.backend.repository.impl;

import com.domu.backend.domain.VoteEvent;
import com.domu.backend.repository.VoteEventRepository;
import com.domu.backend.repository.base.AbstractJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public class VoteEventRepositoryImpl extends AbstractJpaRepository<VoteEvent> implements VoteEventRepository {

    public VoteEventRepositoryImpl() {
        super(VoteEvent.class);
    }
}
