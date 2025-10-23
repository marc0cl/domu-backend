package com.domu.backend.infrastructure.persistence.memory;

import com.domu.backend.domain.core.Role;
import com.domu.backend.infrastructure.persistence.repository.RoleRepository;

public class InMemoryRoleRepository extends InMemoryCrudRepository<Role> implements RoleRepository {
}
