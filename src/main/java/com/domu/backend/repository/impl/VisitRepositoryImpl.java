package com.domu.backend.repository.impl;

import com.domu.backend.domain.Visit;
import com.domu.backend.repository.VisitRepository;
import com.domu.backend.repository.base.AbstractJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public class VisitRepositoryImpl extends AbstractJpaRepository<Visit> implements VisitRepository {

    public VisitRepositoryImpl() {
        super(Visit.class);
    }
}
