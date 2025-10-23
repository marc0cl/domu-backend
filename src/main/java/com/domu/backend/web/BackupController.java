package com.domu.backend.web;

import com.domu.backend.domain.BackupRecord;
import com.domu.backend.domain.Community;
import com.domu.backend.dto.BackupRecordRequest;
import com.domu.backend.repository.BackupRecordRepository;
import com.domu.backend.repository.CommunityRepository;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/backups")
public class BackupController {

    private final BackupRecordRepository backupRecordRepository;
    private final CommunityRepository communityRepository;

    public BackupController(BackupRecordRepository backupRecordRepository,
                            CommunityRepository communityRepository) {
        this.backupRecordRepository = backupRecordRepository;
        this.communityRepository = communityRepository;
    }

    @PostMapping
    public BackupRecord createBackupRecord(@Valid @RequestBody BackupRecordRequest request) {
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

    @GetMapping
    public List<BackupRecord> listBackupRecords() {
        return backupRecordRepository.findAll();
    }
}
