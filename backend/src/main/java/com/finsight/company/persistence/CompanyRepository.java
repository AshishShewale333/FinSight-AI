package com.finsight.company.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CompanyRepository extends JpaRepository<CompanyEntity, String> {
    Page<CompanyEntity> findByActiveTrue(Pageable pageable);
    List<CompanyEntity> findByActiveTrueAndSymbolIn(Collection<String> symbols);
    Page<CompanyEntity> findByActiveTrueAndCompanyNameContainingIgnoreCaseOrActiveTrueAndSymbolContainingIgnoreCase(
            String name, String symbol, Pageable pageable);
}
