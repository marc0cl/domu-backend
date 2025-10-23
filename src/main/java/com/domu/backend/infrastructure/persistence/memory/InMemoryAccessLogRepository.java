package com.domu.backend.infrastructure.persistence.memory;

import com.domu.backend.domain.access.AccessLog;
import com.domu.backend.infrastructure.persistence.repository.AccessLogRepository;

public class InMemoryAccessLogRepository extends InMemoryCrudRepository<AccessLog> implements AccessLogRepository {
}
