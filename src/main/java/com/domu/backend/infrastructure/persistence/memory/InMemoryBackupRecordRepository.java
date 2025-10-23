package com.domu.backend.infrastructure.persistence.memory;

import com.domu.backend.domain.operations.BackupRecord;
import com.domu.backend.infrastructure.persistence.repository.BackupRecordRepository;

public class InMemoryBackupRecordRepository extends InMemoryCrudRepository<BackupRecord> implements BackupRecordRepository {
}
