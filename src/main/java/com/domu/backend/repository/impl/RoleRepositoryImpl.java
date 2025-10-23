package com.domu.backend.repository.impl;

import com.domu.backend.domain.Role;
import com.domu.backend.repository.RoleRepository;
import com.domu.backend.repository.base.AbstractJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public class RoleRepositoryImpl extends AbstractJpaRepository<Role> implements RoleRepository {

    public RoleRepositoryImpl() {
        super(Role.class);
    }
}
