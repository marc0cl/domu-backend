package com.domu.backend.repository;

import com.domu.backend.domain.ProviderRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProviderRequestRepository extends JpaRepository<ProviderRequest, Long> {
}
