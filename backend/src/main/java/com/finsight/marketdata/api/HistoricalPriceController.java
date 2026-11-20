package com.finsight.marketdata.api;

import com.finsight.common.api.ApiResponse;
import com.finsight.common.config.CorrelationIdFilter;
import com.finsight.marketdata.application.HistoricalPriceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.Instant;
import java.util.List;

@RestController
@Validated
@RequestMapping("/api/v1/companies/{companyId}/prices")
public class HistoricalPriceController {
    private final HistoricalPriceService service;
    public HistoricalPriceController(HistoricalPriceService service){this.service=service;}
    @GetMapping
    public ApiResponse<List<HistoricalPriceResponse>> prices(@PathVariable String companyId,
            @RequestParam Instant from,@RequestParam Instant to,
            @RequestParam(defaultValue="1d") @Pattern(regexp="1d") String interval,
            HttpServletRequest request){
        return ApiResponse.success("Historical prices retrieved",service.prices(companyId,from,to),
                request.getHeader(CorrelationIdFilter.HEADER_NAME));
    }
}
