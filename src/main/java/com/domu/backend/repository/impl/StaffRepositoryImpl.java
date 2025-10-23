package com.domu.backend.repository.impl;

import com.domu.backend.domain.Staff;
import com.domu.backend.repository.StaffRepository;
import com.domu.backend.repository.base.AbstractJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public class StaffRepositoryImpl extends AbstractJpaRepository<Staff> implements StaffRepository {

    public StaffRepositoryImpl() {
        super(Staff.class);
    }
}
