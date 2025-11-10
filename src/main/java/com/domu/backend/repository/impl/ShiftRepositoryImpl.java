package com.domu.backend.repository.impl;

import com.domu.backend.domain.Shift;
import com.domu.backend.repository.ShiftRepository;
import com.domu.backend.repository.base.AbstractJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public class ShiftRepositoryImpl extends AbstractJpaRepository<Shift> implements ShiftRepository {

    public ShiftRepositoryImpl() {
        super(Shift.class);
    }
}
