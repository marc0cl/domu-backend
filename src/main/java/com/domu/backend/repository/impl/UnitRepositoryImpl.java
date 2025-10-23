package com.domu.backend.repository.impl;

import com.domu.backend.domain.Unit;
import com.domu.backend.repository.UnitRepository;
import com.domu.backend.repository.base.AbstractJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public class UnitRepositoryImpl extends AbstractJpaRepository<Unit> implements UnitRepository {

    public UnitRepositoryImpl() {
        super(Unit.class);
    }
}
