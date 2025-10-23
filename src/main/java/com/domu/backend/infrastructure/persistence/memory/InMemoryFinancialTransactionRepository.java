package com.domu.backend.infrastructure.persistence.memory;

import com.domu.backend.domain.vendor.FinancialTransaction;
import com.domu.backend.infrastructure.persistence.repository.FinancialTransactionRepository;

public class InMemoryFinancialTransactionRepository extends InMemoryCrudRepository<FinancialTransaction> implements FinancialTransactionRepository {
}
