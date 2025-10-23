package com.domu.backend.repository.impl;

import com.domu.backend.domain.ExpenseStatement;
import com.domu.backend.repository.ExpenseStatementRepository;
import com.domu.backend.repository.base.AbstractJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public class ExpenseStatementRepositoryImpl extends AbstractJpaRepository<ExpenseStatement> implements ExpenseStatementRepository {

    public ExpenseStatementRepositoryImpl() {
        super(ExpenseStatement.class);
    }
}
