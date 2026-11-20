package com.finsight.news.application;

import com.finsight.news.domain.FinancialNewsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name="finsight.news.sync-enabled",havingValue="true")
public class FinancialNewsSyncJob {
    private static final Logger log=LoggerFactory.getLogger(FinancialNewsSyncJob.class);
    private final FinancialNewsProvider provider;
    private final NewsIngestionWriter writer;
    public FinancialNewsSyncJob(FinancialNewsProvider provider,NewsIngestionWriter writer){
        this.provider=provider;this.writer=writer;
    }
    @Scheduled(initialDelayString="${NEWS_SYNC_INITIAL_DELAY:10000}",fixedDelayString="${NEWS_SYNC_FIXED_DELAY:3600000}")
    public void sync(){
        try {
            var items=provider.latestMarketNews();
            items.forEach(item->writer.ingest(null,provider,item));
            log.info("News sync completed provider={} received={}",provider.name(),items.size());
        } catch(Exception exception) {
            log.warn("News sync failed provider={}",provider.name(),exception);
        }
    }
}
