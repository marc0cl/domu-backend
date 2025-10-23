package com.domu.backend.repository.impl;

import com.domu.backend.domain.ParkingSpace;
import com.domu.backend.repository.ParkingSpaceRepository;
import com.domu.backend.repository.base.AbstractJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public class ParkingSpaceRepositoryImpl extends AbstractJpaRepository<ParkingSpace> implements ParkingSpaceRepository {

    public ParkingSpaceRepositoryImpl() {
        super(ParkingSpace.class);
    }
}
