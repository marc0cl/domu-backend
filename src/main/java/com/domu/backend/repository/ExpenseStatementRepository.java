package com.domu.backend.repository;

import com.domu.backend.domain.ExpenseStatement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseStatementRepository extends JpaRepository<ExpenseStatement, Long> {
}
