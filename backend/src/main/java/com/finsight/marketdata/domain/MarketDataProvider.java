package com.finsight.marketdata.domain;

import com.finsight.company.persistence.CompanyEntity;
import java.util.Optional;

public interface MarketDataProvider {
    Optional<MarketQuote> latestQuote(CompanyEntity company);
    String providerName();
}
