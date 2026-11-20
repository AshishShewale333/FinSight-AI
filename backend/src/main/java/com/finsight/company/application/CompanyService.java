package com.finsight.company.application;

import com.finsight.common.error.ApiException;
import com.finsight.company.api.CompanyResponse;
import com.finsight.company.persistence.CompanyEntity;
import com.finsight.company.persistence.CompanyRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanyService {
    private final CompanyRepository repository;
    public CompanyService(CompanyRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public Page<CompanyResponse> search(String query, int page, int size) {
        String term = query == null ? "" : query.trim();
        if (term.length() > 120) throw new ApiException(HttpStatus.BAD_REQUEST, "QUERY_TOO_LONG", "Search query is too long");
        PageRequest pageable = PageRequest.of(page, size, Sort.by("companyName").ascending());
        Page<CompanyEntity> results = term.isBlank()
                ? repository.findByActiveTrue(pageable)
                : repository.findByActiveTrueAndCompanyNameContainingIgnoreCaseOrActiveTrueAndSymbolContainingIgnoreCase(term, term, pageable);
        return results.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public CompanyResponse get(String id) {
        return repository.findById(id).map(this::toResponse)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "COMPANY_NOT_FOUND", "Company was not found"));
    }

    private CompanyResponse toResponse(CompanyEntity company) {
        return new CompanyResponse(company.getId(), company.getExchangeCode(), company.getSymbol(),
                company.getCompanyName(), company.getIsin(), company.getCurrency(), company.getSector(), company.isActive());
    }
}
