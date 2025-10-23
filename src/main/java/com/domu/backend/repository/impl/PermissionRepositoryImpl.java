package com.domu.backend.repository.impl;

import com.domu.backend.domain.Permission;
import com.domu.backend.repository.PermissionRepository;
import com.domu.backend.repository.base.AbstractJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public class PermissionRepositoryImpl extends AbstractJpaRepository<Permission> implements PermissionRepository {

    public PermissionRepositoryImpl() {
        super(Permission.class);
    }
}
