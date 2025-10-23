package com.domu.backend.service;

import com.domu.backend.domain.finance.CommonCharge;
import com.domu.backend.domain.finance.CommonExpensePeriod;
import com.domu.backend.domain.finance.CommonPayment;
import com.domu.backend.domain.finance.DelinquencyRecord;
import com.domu.backend.infrastructure.persistence.repository.CommonChargeRepository;
import com.domu.backend.infrastructure.persistence.repository.CommonExpensePeriodRepository;
import com.domu.backend.infrastructure.persistence.repository.CommonPaymentRepository;
import com.domu.backend.infrastructure.persistence.repository.DelinquencyRecordRepository;

import java.util.List;

public class FinanceService {

    private final CommonExpensePeriodRepository expensePeriodRepository;
    private final CommonChargeRepository commonChargeRepository;
    private final CommonPaymentRepository commonPaymentRepository;
    private final DelinquencyRecordRepository delinquencyRecordRepository;

    public FinanceService(CommonExpensePeriodRepository expensePeriodRepository,
                          CommonChargeRepository commonChargeRepository,
                          CommonPaymentRepository commonPaymentRepository,
                          DelinquencyRecordRepository delinquencyRecordRepository) {
        this.expensePeriodRepository = expensePeriodRepository;
        this.commonChargeRepository = commonChargeRepository;
        this.commonPaymentRepository = commonPaymentRepository;
        this.delinquencyRecordRepository = delinquencyRecordRepository;
    }

    public CommonExpensePeriod generatePeriod(CommonExpensePeriod period) {
        return expensePeriodRepository.save(period);
    }

    public List<CommonExpensePeriod> listPeriods() {
        return expensePeriodRepository.findAll();
    }

    public CommonCharge addCharge(CommonCharge charge) {
        expensePeriodRepository.findById(charge.periodId())
                .orElseThrow(() -> new ResourceNotFoundException("Common expense period not found"));
        return commonChargeRepository.save(charge);
    }

    public List<CommonCharge> listCharges() {
        return commonChargeRepository.findAll();
    }

    public CommonPayment registerPayment(CommonPayment payment) {
        commonChargeRepository.findById(payment.chargeId())
                .orElseThrow(() -> new ResourceNotFoundException("Charge not found"));
        return commonPaymentRepository.save(payment);
    }

    public List<CommonPayment> listPayments() {
        return commonPaymentRepository.findAll();
    }

    public DelinquencyRecord registerDelinquency(DelinquencyRecord record) {
        return delinquencyRecordRepository.save(record);
    }

    public List<DelinquencyRecord> listDelinquency() {
        return delinquencyRecordRepository.findAll();
    }
}
