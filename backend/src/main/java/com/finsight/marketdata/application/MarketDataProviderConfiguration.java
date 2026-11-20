package com.finsight.marketdata.application;

import com.finsight.company.persistence.CompanyEntity;
import com.finsight.marketdata.domain.MarketDataProvider;
import com.finsight.marketdata.domain.MarketQuote;
import com.finsight.marketdata.domain.QuoteStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

@Configuration
public class MarketDataProviderConfiguration {

    @Bean
    @ConditionalOnProperty(name = "finsight.market-data.provider", havingValue = "mock")
    MarketDataProvider mockMarketDataProvider(MockStockPriceService mockPrices) {
        return new MarketDataProvider() {
            @Override
            public Optional<MarketQuote> latestQuote(CompanyEntity company) {
                return mockPrices.latestQuote(company.getSymbol());
            }
            @Override public String providerName() { return MockStockPriceService.PROVIDER; }
        };
    }

    @Bean
    @ConditionalOnMissingBean(MarketDataProvider.class)
    MarketDataProvider unavailableMarketDataProvider() {
        return new MarketDataProvider() {
            @Override public Optional<MarketQuote> latestQuote(CompanyEntity company) { return Optional.empty(); }
            @Override public String providerName() { return "UNAVAILABLE"; }
        };
    }
}
