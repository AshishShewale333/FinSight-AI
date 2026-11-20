package com.finsight.company.api;

import com.finsight.common.api.ApiResponse;
import com.finsight.common.config.CorrelationIdFilter;
import com.finsight.company.application.CompanyService;
import com.finsight.company.application.CompanyComparisonService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/companies")
public class CompanyController {
    private final CompanyService companyService;
    private final CompanyComparisonService comparisonService;
    public CompanyController(CompanyService companyService, CompanyComparisonService comparisonService) {
        this.companyService = companyService; this.comparisonService = comparisonService;
    }

    @GetMapping("/compare")
    public ApiResponse<java.util.List<CompanyComparisonItem>> compare(@RequestParam String ids, HttpServletRequest request) {
        return ApiResponse.success("Company comparison retrieved", comparisonService.compare(ids),
                request.getHeader(CorrelationIdFilter.HEADER_NAME));
    }

    @GetMapping
    public ApiResponse<Page<CompanyResponse>> search(@RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int size,
            HttpServletRequest request) {
        return ApiResponse.success("Companies retrieved", companyService.search(query, page, size),
                request.getHeader(CorrelationIdFilter.HEADER_NAME));
    }

    @GetMapping("/{companyId}")
    public ApiResponse<CompanyResponse> get(@PathVariable String companyId, HttpServletRequest request) {
        return ApiResponse.success("Company retrieved", companyService.get(companyId),
                request.getHeader(CorrelationIdFilter.HEADER_NAME));
    }
}
