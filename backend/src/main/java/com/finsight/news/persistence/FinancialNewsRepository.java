package com.finsight.news.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface FinancialNewsRepository extends JpaRepository<FinancialNewsEntity,String> {
    Page<FinancialNewsEntity> findAllByOrderByPublishedAtDesc(Pageable pageable);
    Page<FinancialNewsEntity> findByCompanyIdOrderByPublishedAtDesc(String companyId,Pageable pageable);
    Optional<FinancialNewsEntity> findByIdAndSummaryStatus(String id,String status);
    boolean existsBySourceAndExternalId(String source,String externalId);
}
