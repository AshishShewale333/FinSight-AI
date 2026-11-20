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
    MarketDataProvider mockMarketDataProvider() {
        return new MarketDataProvider() {
            @Override
            public Optional<MarketQuote> latestQuote(CompanyEntity company) {
                int seed = Math.floorMod(company.getSymbol().hashCode(), 100_000);
                BigDecimal mockPrice = BigDecimal.valueOf(100 + seed, 2);
                return Optional.of(new MarketQuote(company.getId(), mockPrice, company.getCurrency(),
                        "LOCAL_DEMO_MOCK", QuoteStatus.MOCK, Instant.now()));
            }
            @Override public String providerName() { return "LOCAL_DEMO_MOCK"; }
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
