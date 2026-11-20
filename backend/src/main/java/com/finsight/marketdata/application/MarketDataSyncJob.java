package com.finsight.marketdata.application;

import com.finsight.company.persistence.CompanyRepository;
import com.finsight.company.persistence.CompanyEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(name="finsight.market-data.sync-enabled",havingValue="true")
public class MarketDataSyncJob {
    private static final Logger log=LoggerFactory.getLogger(MarketDataSyncJob.class);
    private final CompanyRepository companies;
    private final MarketDataService marketData;
    public MarketDataSyncJob(CompanyRepository companies,MarketDataService marketData){this.companies=companies;this.marketData=marketData;}
    @Scheduled(fixedDelayString="${MARKET_DATA_SYNC_FIXED_DELAY:300000}")
    public void sync(){
        List<CompanyEntity> active = companies.findByActiveTrue(PageRequest.of(0,500)).getContent();
        int failures = 0;
        for (CompanyEntity company : active) {
            try { marketData.refreshForCompany(company.getId()); }
            catch (Exception exception) {
                failures++;
                log.warn("Market data refresh failed companyId={}", company.getId(), exception);
            }
        }
        log.info("Market data sync completed attempted={} failures={}", active.size(), failures);
    }
}
