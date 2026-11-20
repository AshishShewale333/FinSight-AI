package com.finsight.marketdata.application;

import com.finsight.common.error.ApiException;
import com.finsight.infrastructure.outbox.OutboxService;
import com.finsight.company.persistence.CompanyEntity;
import com.finsight.company.persistence.CompanyRepository;
import com.finsight.marketdata.api.QuoteResponse;
import com.finsight.marketdata.domain.MarketDataProvider;
import com.finsight.marketdata.domain.MarketQuote;
import com.finsight.marketdata.persistence.StockQuoteEntity;
import com.finsight.marketdata.persistence.StockQuoteRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

@Service
public class MarketDataService {
    private final CompanyRepository companies;
    private final StockQuoteRepository quotes;
    private final MarketDataProvider provider;
    private final OutboxService outbox;
    private final Clock clock;

    public MarketDataService(CompanyRepository companies, StockQuoteRepository quotes,
                             MarketDataProvider provider, OutboxService outbox, Clock clock) {
        this.companies = companies; this.quotes = quotes; this.provider = provider; this.outbox=outbox; this.clock = clock;
    }

    @Transactional
    @Cacheable(cacheNames = "latestQuotes", key = "#companyId")
    public QuoteResponse latestQuote(String companyId) {
        CompanyEntity company = companies.findById(companyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "COMPANY_NOT_FOUND", "Company was not found"));
        MarketQuote quote = provider.latestQuote(company)
                .orElseThrow(() -> new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "MARKET_DATA_UNAVAILABLE",
                        "No market data provider is configured; no quote is available"));
        StockQuoteEntity saved = quotes.save(new StockQuoteEntity(UUID.randomUUID().toString(), company,
                quote.price(), quote.currency(), quote.provider(), quote.status(), quote.observedAt()));
        if (quote.status() != com.finsight.marketdata.domain.QuoteStatus.MOCK) {
            outbox.record("Company", companyId, "StockQuoteUpdated", java.util.Map.of(
                    "companyId", companyId, "price", quote.price(), "currency", quote.currency(),
                    "status", quote.status().name(), "observedAt", quote.observedAt()));
        }
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public QuoteResponse lastStoredQuote(String companyId) {
        StockQuoteEntity quote = quotes.findFirstByCompanyIdOrderByObservedAtDesc(companyId)
                .orElseThrow(() -> new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "MARKET_DATA_UNAVAILABLE",
                        "No quote has been stored for this company"));
        return toResponse(quote);
    }

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(cacheNames="latestQuotes",key="#companyId")
    public void refreshForCompany(String companyId) {
        CompanyEntity company=companies.findById(companyId).orElse(null);
        if(company==null||!company.isActive())return;
        provider.latestQuote(company).ifPresent(quote->{
            quotes.save(new StockQuoteEntity(UUID.randomUUID().toString(),company,quote.price(),quote.currency(),
                    quote.provider(),quote.status(),quote.observedAt()));
            if(quote.status()!=com.finsight.marketdata.domain.QuoteStatus.MOCK){
                outbox.record("Company",companyId,"StockQuoteUpdated",java.util.Map.of("companyId",companyId,
                        "price",quote.price(),"currency",quote.currency(),"status",quote.status().name(),
                        "observedAt",quote.observedAt()));
            }
        });
    }

    private QuoteResponse toResponse(StockQuoteEntity quote) {
        boolean stale = Duration.between(quote.getObservedAt(), clock.instant()).compareTo(Duration.ofMinutes(15)) > 0;
        return new QuoteResponse(quote.getCompany().getId(), quote.getPrice(), quote.getCurrency(),
                quote.getProvider(), quote.getStatus().name(), quote.getObservedAt(), stale);
    }
}
