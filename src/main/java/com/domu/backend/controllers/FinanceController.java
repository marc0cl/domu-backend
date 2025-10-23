package com.domu.backend.controllers;

import com.domu.backend.domain.ExpenseStatement;
import com.domu.backend.domain.Payment;
import com.domu.backend.dto.ExpenseStatementRequest;
import com.domu.backend.dto.PaymentRequest;
import com.domu.backend.services.FinanceService;
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

    private final FinanceService financeService;

    public FinanceController(FinanceService financeService) {
        this.financeService = financeService;
    }

    @PostMapping("/statements")
    public ExpenseStatement createExpenseStatement(@Valid @RequestBody ExpenseStatementRequest request) {
        return financeService.createExpenseStatement(request);
    }

    @GetMapping("/statements")
    public List<ExpenseStatement> listStatements() {
        return financeService.listStatements();
    }

    @PostMapping("/payments")
    public Payment createPayment(@Valid @RequestBody PaymentRequest request) {
        return financeService.createPayment(request);
    }

    @GetMapping("/payments")
    public List<Payment> listPayments() {
        return financeService.listPayments();
    }
}
