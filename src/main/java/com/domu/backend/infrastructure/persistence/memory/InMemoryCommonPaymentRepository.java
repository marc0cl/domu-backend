package com.domu.backend.infrastructure.persistence.memory;

import com.domu.backend.domain.finance.CommonPayment;
import com.domu.backend.infrastructure.persistence.repository.CommonPaymentRepository;

public class InMemoryCommonPaymentRepository extends InMemoryCrudRepository<CommonPayment> implements CommonPaymentRepository {
}
