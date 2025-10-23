package com.domu.backend.repository.impl;

import com.domu.backend.domain.ParkingPermit;
import com.domu.backend.repository.ParkingPermitRepository;
import com.domu.backend.repository.base.AbstractJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public class ParkingPermitRepositoryImpl extends AbstractJpaRepository<ParkingPermit> implements ParkingPermitRepository {

    public ParkingPermitRepositoryImpl() {
        super(ParkingPermit.class);
    }
}
