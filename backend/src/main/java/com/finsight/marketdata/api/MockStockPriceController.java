package com.finsight.marketdata.api;

import com.finsight.common.api.ApiResponse;
import com.finsight.common.config.CorrelationIdFilter;
import com.finsight.marketdata.application.MockStockPriceService;
import com.finsight.stockexplorer.api.StockExplorerListingResponse;
import com.finsight.stockexplorer.application.StockExplorerService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@ConditionalOnProperty(name = "finsight.market-data.provider", havingValue = "mock")
@RequestMapping("/api/v1/market/mock-stocks")
public class MockStockPriceController {
    private final MockStockPriceService prices;
    private final StockExplorerService listings;

    public MockStockPriceController(MockStockPriceService prices, StockExplorerService listings) {
        this.prices = prices;
        this.listings = listings;
    }

    @GetMapping
    public ApiResponse<List<MockStockPriceResponse>> latest(HttpServletRequest request) {
        return ApiResponse.success("Mock stock prices retrieved", prices.latestPrices(), cid(request));
    }

    @GetMapping("/tradable-listings")
    public ApiResponse<List<StockExplorerListingResponse>> tradableListings(HttpServletRequest request) {
        List<String> symbols = prices.latestPrices().stream().map(MockStockPriceResponse::symbol).toList();
        return ApiResponse.success("Live priced NSE listings retrieved", listings.activeBySymbols(symbols), cid(request));
    }

    @GetMapping("/{symbol}/history")
    public ApiResponse<List<MockStockPriceResponse>> history(@PathVariable String symbol,
                                                              HttpServletRequest request) {
        return ApiResponse.success("Mock stock price history retrieved", prices.history(symbol), cid(request));
    }

    private String cid(HttpServletRequest request) {
        return request.getHeader(CorrelationIdFilter.HEADER_NAME);
    }
}
