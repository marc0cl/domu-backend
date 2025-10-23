package com.domu.backend.repository.impl;

import com.domu.backend.domain.Amenity;
import com.domu.backend.repository.AmenityRepository;
import com.domu.backend.repository.base.AbstractJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public class AmenityRepositoryImpl extends AbstractJpaRepository<Amenity> implements AmenityRepository {

    public AmenityRepositoryImpl() {
        super(Amenity.class);
    }
}
