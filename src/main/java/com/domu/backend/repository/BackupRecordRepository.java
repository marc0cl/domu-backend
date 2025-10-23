package com.domu.backend.repository;

import com.domu.backend.domain.BackupRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BackupRecordRepository extends JpaRepository<BackupRecord, Long> {
}
