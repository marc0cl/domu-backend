package com.domu.backend.repository.impl;

import com.domu.backend.domain.Building;
import com.domu.backend.repository.BuildingRepository;
import com.domu.backend.repository.base.AbstractJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public class BuildingRepositoryImpl extends AbstractJpaRepository<Building> implements BuildingRepository {

    public BuildingRepositoryImpl() {
        super(Building.class);
    }
}
