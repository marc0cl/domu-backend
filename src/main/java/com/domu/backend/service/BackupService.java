package com.domu.backend.service;

import com.domu.backend.domain.operations.BackupRecord;
import com.domu.backend.infrastructure.persistence.repository.BackupRecordRepository;

import java.util.List;

public class BackupService {

    private final BackupRecordRepository backupRecordRepository;

    public BackupService(BackupRecordRepository backupRecordRepository) {
        this.backupRecordRepository = backupRecordRepository;
    }

    public BackupRecord registerBackup(BackupRecord record) {
        return backupRecordRepository.save(record);
    }

    public List<BackupRecord> listBackups() {
        return backupRecordRepository.findAll();
    }
}
