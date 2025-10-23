package com.domu.backend.repository.impl;

import com.domu.backend.domain.Payment;
import com.domu.backend.repository.PaymentRepository;
import com.domu.backend.repository.base.AbstractJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentRepositoryImpl extends AbstractJpaRepository<Payment> implements PaymentRepository {

    public PaymentRepositoryImpl() {
        super(Payment.class);
    }
}
