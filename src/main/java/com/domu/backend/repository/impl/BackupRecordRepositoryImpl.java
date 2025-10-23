package com.domu.backend.repository.impl;

import com.domu.backend.domain.BackupRecord;
import com.domu.backend.repository.BackupRecordRepository;
import com.domu.backend.repository.base.AbstractJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public class BackupRecordRepositoryImpl extends AbstractJpaRepository<BackupRecord> implements BackupRecordRepository {

    public BackupRecordRepositoryImpl() {
        super(BackupRecord.class);
    }
}
