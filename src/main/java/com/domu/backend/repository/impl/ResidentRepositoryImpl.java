package com.domu.backend.repository.impl;

import com.domu.backend.domain.Resident;
import com.domu.backend.repository.ResidentRepository;
import com.domu.backend.repository.base.AbstractJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public class ResidentRepositoryImpl extends AbstractJpaRepository<Resident> implements ResidentRepository {

    public ResidentRepositoryImpl() {
        super(Resident.class);
    }
}
