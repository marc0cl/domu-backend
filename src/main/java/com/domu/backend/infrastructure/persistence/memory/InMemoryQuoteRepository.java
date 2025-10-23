package com.domu.backend.infrastructure.persistence.memory;

import com.domu.backend.domain.vendor.Quote;
import com.domu.backend.infrastructure.persistence.repository.QuoteRepository;

public class InMemoryQuoteRepository extends InMemoryCrudRepository<Quote> implements QuoteRepository {
}
