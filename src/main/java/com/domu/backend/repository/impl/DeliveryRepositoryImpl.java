package com.domu.backend.repository.impl;

import com.domu.backend.domain.Delivery;
import com.domu.backend.repository.DeliveryRepository;
import com.domu.backend.repository.base.AbstractJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public class DeliveryRepositoryImpl extends AbstractJpaRepository<Delivery> implements DeliveryRepository {

    public DeliveryRepositoryImpl() {
        super(Delivery.class);
    }
}
