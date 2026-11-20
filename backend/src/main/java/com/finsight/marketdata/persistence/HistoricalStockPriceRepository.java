package com.finsight.marketdata.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;

public interface HistoricalStockPriceRepository extends JpaRepository<HistoricalStockPriceEntity, String> {
    List<HistoricalStockPriceEntity> findByCompanyIdAndIntervalCodeAndPriceTimeBetweenOrderByPriceTimeAsc(
            String companyId, String intervalCode, Instant from, Instant to);
}
