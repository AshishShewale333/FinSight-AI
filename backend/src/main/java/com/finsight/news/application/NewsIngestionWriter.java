package com.finsight.news.application;

import com.finsight.company.persistence.CompanyEntity;
import com.finsight.infrastructure.outbox.OutboxService;
import com.finsight.news.domain.FinancialNewsProvider;
import com.finsight.news.persistence.FinancialNewsEntity;
import com.finsight.news.persistence.FinancialNewsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;

@Service
public class NewsIngestionWriter {
    private final FinancialNewsRepository news;
    private final OutboxService outbox;
    private final boolean autoSummarize;
    private final Clock clock;
    public NewsIngestionWriter(FinancialNewsRepository news,OutboxService outbox,Clock clock,
            @Value("${finsight.news.auto-summarize:false}") boolean autoSummarize){
        this.news=news;this.outbox=outbox;this.clock=clock;this.autoSummarize=autoSummarize;
    }
    @Transactional
    public void ingest(CompanyEntity company,FinancialNewsProvider provider,FinancialNewsProvider.NewsItem item){
        if(item.externalId()==null||item.externalId().isBlank()||item.title()==null||item.title().isBlank()
                ||item.url()==null||!(item.url().startsWith("https://")||item.url().startsWith("http://"))) return;
        String source=item.source()==null||item.source().isBlank()?provider.name():item.source();
        source=source.substring(0,Math.min(100,source.length()));
        if(news.existsBySourceAndExternalId(source,item.externalId()))return;
        String content=item.content();
        if(content!=null&&content.length()>100000)content=content.substring(0,100000);
        String id=UUID.randomUUID().toString();
        news.save(new FinancialNewsEntity(id,company,source,item.externalId(),item.title().substring(0,Math.min(500,item.title().length())),
                item.url().substring(0,Math.min(1000,item.url().length())),item.publishedAt(),content,clock.instant()));
        if(autoSummarize) outbox.record("FinancialNews",id,"FinancialNewsIngested",Map.of("newsId",id));
    }
}
