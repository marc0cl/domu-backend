package com.domu.backend.service;

import com.domu.backend.domain.vendor.FinancialTransaction;
import com.domu.backend.domain.vendor.Invoice;
import com.domu.backend.domain.vendor.PreventiveMaintenance;
import com.domu.backend.domain.vendor.Provider;
import com.domu.backend.domain.vendor.Quote;
import com.domu.backend.domain.vendor.ServiceRequest;
import com.domu.backend.infrastructure.persistence.repository.FinancialTransactionRepository;
import com.domu.backend.infrastructure.persistence.repository.InvoiceRepository;
import com.domu.backend.infrastructure.persistence.repository.PreventiveMaintenanceRepository;
import com.domu.backend.infrastructure.persistence.repository.ProviderRepository;
import com.domu.backend.infrastructure.persistence.repository.QuoteRepository;
import com.domu.backend.infrastructure.persistence.repository.ServiceRequestRepository;

import java.util.List;

public class VendorService {

    private final ProviderRepository providerRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final QuoteRepository quoteRepository;
    private final InvoiceRepository invoiceRepository;
    private final PreventiveMaintenanceRepository preventiveMaintenanceRepository;
    private final FinancialTransactionRepository financialTransactionRepository;

    public VendorService(ProviderRepository providerRepository,
                         ServiceRequestRepository serviceRequestRepository,
                         QuoteRepository quoteRepository,
                         InvoiceRepository invoiceRepository,
                         PreventiveMaintenanceRepository preventiveMaintenanceRepository,
                         FinancialTransactionRepository financialTransactionRepository) {
        this.providerRepository = providerRepository;
        this.serviceRequestRepository = serviceRequestRepository;
        this.quoteRepository = quoteRepository;
        this.invoiceRepository = invoiceRepository;
        this.preventiveMaintenanceRepository = preventiveMaintenanceRepository;
        this.financialTransactionRepository = financialTransactionRepository;
    }

    public Provider registerProvider(Provider provider) {
        return providerRepository.save(provider);
    }

    public List<Provider> listProviders() {
        return providerRepository.findAll();
    }

    public ServiceRequest createServiceRequest(ServiceRequest request) {
        providerRepository.findById(request.providerId())
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found"));
        return serviceRequestRepository.save(request);
    }

    public List<ServiceRequest> listServiceRequests() {
        return serviceRequestRepository.findAll();
    }

    public Quote registerQuote(Quote quote) {
        serviceRequestRepository.findById(quote.serviceRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("Service request not found"));
        return quoteRepository.save(quote);
    }

    public List<Quote> listQuotes() {
        return quoteRepository.findAll();
    }

    public Invoice registerInvoice(Invoice invoice) {
        serviceRequestRepository.findById(invoice.serviceRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("Service request not found"));
        return invoiceRepository.save(invoice);
    }

    public List<Invoice> listInvoices() {
        return invoiceRepository.findAll();
    }

    public PreventiveMaintenance scheduleMaintenance(PreventiveMaintenance maintenance) {
        providerRepository.findById(maintenance.providerId())
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found"));
        return preventiveMaintenanceRepository.save(maintenance);
    }

    public List<PreventiveMaintenance> listPreventiveMaintenance() {
        return preventiveMaintenanceRepository.findAll();
    }

    public FinancialTransaction recordTransaction(FinancialTransaction transaction) {
        providerRepository.findById(transaction.providerId())
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found"));
        return financialTransactionRepository.save(transaction);
    }

    public List<FinancialTransaction> listTransactions() {
        return financialTransactionRepository.findAll();
    }
}
