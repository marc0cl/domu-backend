package com.domu.backend.infrastructure.persistence.memory;

import com.domu.backend.domain.vendor.Invoice;
import com.domu.backend.infrastructure.persistence.repository.InvoiceRepository;

public class InMemoryInvoiceRepository extends InMemoryCrudRepository<Invoice> implements InvoiceRepository {
}
