package com.finsight.news.application;

import com.finsight.news.domain.FinancialNewsProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NewsProviderConfiguration {
    @Bean
    @ConditionalOnMissingBean(FinancialNewsProvider.class)
    FinancialNewsProvider unavailableNewsProvider() {
        return new FinancialNewsProvider() {
            @Override public java.util.List<NewsItem> latestMarketNews() { return java.util.List.of(); }
            @Override public String name() { return "UNAVAILABLE"; }
        };
    }
}
