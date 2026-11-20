package com.finsight.marketdata.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StockQuoteRepository extends JpaRepository<StockQuoteEntity, String> {
    Optional<StockQuoteEntity> findFirstByCompanyIdOrderByObservedAtDesc(String companyId);
}
