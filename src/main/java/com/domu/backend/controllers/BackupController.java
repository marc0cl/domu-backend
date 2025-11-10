package com.domu.backend.controllers;

import com.domu.backend.domain.BackupRecord;
import com.domu.backend.dto.BackupRecordRequest;
import com.domu.backend.services.BackupService;
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

    private final BackupService backupService;

    public BackupController(BackupService backupService) {
        this.backupService = backupService;
    }

    @PostMapping
    public BackupRecord createBackupRecord(@Valid @RequestBody BackupRecordRequest request) {
        return backupService.createBackupRecord(request);
    }

    @GetMapping
    public List<BackupRecord> listBackupRecords() {
        return backupService.listBackupRecords();
    }
}
