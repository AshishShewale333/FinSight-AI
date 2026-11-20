package com.finsight.company.metrics;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CompanyFinancialMetricsRepository extends JpaRepository<CompanyFinancialMetricsEntity,String> {
    Optional<CompanyFinancialMetricsEntity> findFirstByCompanyIdOrderByPeriodEndDesc(String companyId);
}
