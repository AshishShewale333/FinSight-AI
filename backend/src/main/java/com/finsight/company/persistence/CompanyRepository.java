package com.finsight.company.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<CompanyEntity, String> {
    Page<CompanyEntity> findByActiveTrue(Pageable pageable);
    Page<CompanyEntity> findByActiveTrueAndCompanyNameContainingIgnoreCaseOrActiveTrueAndSymbolContainingIgnoreCase(
            String name, String symbol, Pageable pageable);
}
