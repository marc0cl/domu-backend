package com.domu.backend.services;

import com.domu.backend.domain.BackupRecord;
import com.domu.backend.domain.Community;
import com.domu.backend.dto.BackupRecordRequest;
import com.domu.backend.exceptions.ResourceNotFoundException;
import com.domu.backend.repository.BackupRecordRepository;
import com.domu.backend.repository.CommunityRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BackupService {

    private final BackupRecordRepository backupRecordRepository;
    private final CommunityRepository communityRepository;

    public BackupService(BackupRecordRepository backupRecordRepository,
                         CommunityRepository communityRepository) {
        this.backupRecordRepository = backupRecordRepository;
        this.communityRepository = communityRepository;
    }

    public BackupRecord createBackupRecord(BackupRecordRequest request) {
        Community community = communityRepository.findById(request.communityId())
                .orElseThrow(() -> new ResourceNotFoundException("Community not found"));
        BackupRecord record = new BackupRecord();
        record.setCommunity(community);
        if (request.backupDate() != null) {
            record.setBackupDate(request.backupDate());
        }
        record.setStorageLocation(request.storageLocation());
        record.setRpoHours(request.rpoHours());
        record.setRtoHours(request.rtoHours());
        record.setLastRestorationTest(request.lastRestorationTest());
        return backupRecordRepository.save(record);
    }

    public List<BackupRecord> listBackupRecords() {
        return backupRecordRepository.findAll();
    }
}
