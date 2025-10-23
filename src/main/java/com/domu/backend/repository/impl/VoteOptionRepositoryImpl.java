package com.domu.backend.repository.impl;

import com.domu.backend.domain.VoteOption;
import com.domu.backend.repository.VoteOptionRepository;
import com.domu.backend.repository.base.AbstractJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public class VoteOptionRepositoryImpl extends AbstractJpaRepository<VoteOption> implements VoteOptionRepository {

    public VoteOptionRepositoryImpl() {
        super(VoteOption.class);
    }
}
