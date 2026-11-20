package com.finsight.stockexplorer.api;

import com.finsight.common.api.ApiResponse;
import com.finsight.common.config.CorrelationIdFilter;
import com.finsight.stockexplorer.application.StockExplorerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/stock-explorer")
public class StockExplorerController {
    private final StockExplorerService service;

    public StockExplorerController(StockExplorerService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<Page<StockExplorerListingResponse>> search(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size,
            HttpServletRequest request) {
        return ApiResponse.success("Stock Explorer securities retrieved", service.search(query, page, size),
                request.getHeader(CorrelationIdFilter.HEADER_NAME));
    }
}
