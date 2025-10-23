package com.domu.backend.controllers;

import com.domu.backend.domain.Community;
import com.domu.backend.domain.ExpenseStatement;
import com.domu.backend.domain.Payment;
import com.domu.backend.domain.Resident;
import com.domu.backend.domain.Unit;
import com.domu.backend.dto.ExpenseStatementRequest;
import com.domu.backend.dto.PaymentRequest;
import com.domu.backend.repository.CommunityRepository;
import com.domu.backend.repository.ExpenseStatementRepository;
import com.domu.backend.repository.PaymentRepository;
import com.domu.backend.repository.ResidentRepository;
import com.domu.backend.repository.UnitRepository;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/finance")
public class FinanceController {

    private final ExpenseStatementRepository expenseStatementRepository;
    private final PaymentRepository paymentRepository;
    private final CommunityRepository communityRepository;
    private final UnitRepository unitRepository;
    private final ResidentRepository residentRepository;

    public FinanceController(ExpenseStatementRepository expenseStatementRepository,
                             PaymentRepository paymentRepository,
                             CommunityRepository communityRepository,
                             UnitRepository unitRepository,
                             ResidentRepository residentRepository) {
        this.expenseStatementRepository = expenseStatementRepository;
        this.paymentRepository = paymentRepository;
        this.communityRepository = communityRepository;
        this.unitRepository = unitRepository;
        this.residentRepository = residentRepository;
    }

    @PostMapping("/statements")
    public ExpenseStatement createExpenseStatement(@Valid @RequestBody ExpenseStatementRequest request) {
        Community community = communityRepository.findById(request.communityId())
                .orElseThrow(() -> new ResourceNotFoundException("Community not found"));
        Unit unit = unitRepository.findById(request.unitId())
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found"));
        ExpenseStatement statement = new ExpenseStatement();
        statement.setCommunity(community);
        statement.setUnit(unit);
        statement.setPeriodStart(request.periodStart());
        statement.setPeriodEnd(request.periodEnd());
        statement.setChargesTotal(request.chargesTotal());
        if (request.paymentsTotal() != null) {
            statement.setPaymentsTotal(request.paymentsTotal());
        }
        statement.setBalanceDue(request.balanceDue());
        statement.setDueDate(request.dueDate());
        if (request.status() != null) {
            statement.setStatus(request.status());
        }
        return expenseStatementRepository.save(statement);
    }

    @GetMapping("/statements")
    public List<ExpenseStatement> listStatements() {
        return expenseStatementRepository.findAll();
    }

    @PostMapping("/payments")
    public Payment createPayment(@Valid @RequestBody PaymentRequest request) {
        ExpenseStatement statement = expenseStatementRepository.findById(request.statementId())
                .orElseThrow(() -> new ResourceNotFoundException("Expense statement not found"));
        Resident resident = residentRepository.findById(request.residentId())
                .orElseThrow(() -> new ResourceNotFoundException("Resident not found"));
        Payment payment = new Payment();
        payment.setStatement(statement);
        payment.setResident(resident);
        payment.setAmount(request.amount());
        payment.setMethod(request.method());
        if (request.paidAt() != null) {
            payment.setPaidAt(request.paidAt());
        }
        payment.setReceiptUrl(request.receiptUrl());
        payment.setTransactionReference(request.transactionReference());
        return paymentRepository.save(payment);
    }

    @GetMapping("/payments")
    public List<Payment> listPayments() {
        return paymentRepository.findAll();
    }
}
